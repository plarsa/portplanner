package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.*;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.*;
import se.portplanner.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class StorageYardService {

    private final StorageYardRepository yardRepo;
    private final WinterSeasonRepository seasonRepo;
    private final StoragePlacementRepository placementRepo;
    private final HaulOutBookingRepository bookingRepo;
    private final StoragePackingGroupRepository groupRepo;
    private final AuditService auditService;

    public StorageYardService(StorageYardRepository yardRepo, WinterSeasonRepository seasonRepo,
                               StoragePlacementRepository placementRepo,
                               HaulOutBookingRepository bookingRepo,
                               StoragePackingGroupRepository groupRepo,
                               AuditService auditService) {
        this.yardRepo = yardRepo;
        this.seasonRepo = seasonRepo;
        this.placementRepo = placementRepo;
        this.bookingRepo = bookingRepo;
        this.groupRepo = groupRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<StorageYardResponse> findBySeason(Long seasonId) {
        return yardRepo.findBySeasonIdOrderByNameAsc(seasonId).stream()
                .map(StorageYardResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StorageYardResponse findById(Long id) {
        return StorageYardResponse.from(getOrThrow(id));
    }

    public StorageYardResponse create(StorageYardRequest req) {
        var season = seasonRepo.findById(req.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Vintersäsong " + req.seasonId() + " hittades inte"));
        var yard = new StorageYard();
        yard.setSeason(season);
        yard.setName(req.name());
        yard.setBackgroundImageUrl(req.backgroundImageUrl());
        if (req.laneMarginM() != null) yard.setLaneMarginM(req.laneMarginM());
        var saved = yardRepo.save(yard);
        auditService.log("CREATED", "STORAGE_YARD", saved.getId(), "Gårdsplan skapad: " + saved.getName());
        return StorageYardResponse.from(saved);
    }

    public StorageYardResponse update(Long id, StorageYardRequest req) {
        var yard = getOrThrow(id);
        yard.setName(req.name());
        yard.setBackgroundImageUrl(req.backgroundImageUrl());
        if (req.laneMarginM() != null) yard.setLaneMarginM(req.laneMarginM());
        return StorageYardResponse.from(yardRepo.save(yard));
    }

    public StorageYardResponse calibrate(Long id, StorageYardCalibrationRequest req) {
        var yard = getOrThrow(id);
        yard.setOriginPixelX(req.originPixelX());
        yard.setOriginPixelY(req.originPixelY());
        yard.setPixelsPerMeter(req.pixelsPerMeter());

        yard.getBoundary().clear();
        if (req.boundary() != null) {
            for (int i = 0; i < req.boundary().size(); i++) {
                var dto = req.boundary().get(i);
                var pt = new StorageYardBoundaryPoint();
                pt.setYard(yard);
                pt.setPointOrder(i);
                pt.setXMeters(dto.xMeters());
                pt.setYMeters(dto.yMeters());
                yard.getBoundary().add(pt);
            }
        }
        auditService.log("CALIBRATED", "STORAGE_YARD", id, "Kalibrering sparad för gårdsplan " + yard.getName());
        return StorageYardResponse.from(yardRepo.save(yard));
    }

    public void delete(Long id) {
        var yard = getOrThrow(id);
        yardRepo.delete(yard);
        auditService.log("DELETED", "STORAGE_YARD", id, "Gårdsplan borttagen: " + yard.getName());
    }

    /** Shelf-packing: sort confirmed bookings by length desc, fill rows from origin. */
    public List<StoragePlacementResponse> suggestPlacements(Long yardId) {
        var yard = getOrThrow(yardId);
        var margin = yard.getLaneMarginM() != null ? yard.getLaneMarginM().doubleValue() : 0.5;

        var bookings = bookingRepo.findBySlotSeasonIdAndStatusIn(
                yard.getSeason().getId(),
                List.of(HaulOutBookingStatus.CONFIRMED));

        // Delete existing PLANNED placements for this yard before regenerating
        var existing = placementRepo.findByYardIdOrderByYMetersAscXMetersAsc(yardId);
        var toDelete = existing.stream()
                .filter(p -> p.getStatus() == StoragePlacementStatus.PLANNED)
                .toList();
        placementRepo.deleteAll(toDelete);
        placementRepo.flush();

        // Sort by length descending for better packing
        var sorted = bookings.stream()
                .sorted(Comparator.comparing(b -> b.getBoat().getLengthM(), Comparator.reverseOrder()))
                .toList();

        double x = margin;
        double y = margin;
        double rowMaxLength = 0;

        // Estimate available width from boundary or use a generous default
        double availableWidth = estimateWidth(yard);

        var results = new ArrayList<StoragePlacement>();
        for (var booking : sorted) {
            var boat = booking.getBoat();
            double boatWidth = boat.getWidthM().doubleValue();
            double boatLength = boat.getLengthM().doubleValue();

            // If this boat doesn't fit in current row, move to next row
            if (x + boatWidth + margin > availableWidth && results.size() > 0) {
                y += rowMaxLength + margin;
                x = margin;
                rowMaxLength = 0;
            }

            var placement = new StoragePlacement();
            placement.setYard(yard);
            placement.setBooking(booking);
            placement.setXMeters(BigDecimal.valueOf(x).setScale(3, java.math.RoundingMode.HALF_UP));
            placement.setYMeters(BigDecimal.valueOf(y).setScale(3, java.math.RoundingMode.HALF_UP));
            placement.setRotationDeg(BigDecimal.ZERO);
            placement.setWidthMeters(boat.getWidthM());
            placement.setLengthMeters(boat.getLengthM());
            placement.setStatus(StoragePlacementStatus.PLANNED);
            results.add(placement);

            x += boatWidth + margin;
            rowMaxLength = Math.max(rowMaxLength, boatLength);
        }

        var saved = placementRepo.saveAll(results);
        auditService.log("SUGGESTED", "STORAGE_YARD", yardId,
                "Placeringsförslag genererat: " + saved.size() + " båtar");
        return saved.stream().map(StoragePlacementResponse::from).toList();
    }

    /** Validate all placements: collision check with margin. */
    @Transactional(readOnly = true)
    public List<ValidationIssue> validate(Long yardId) {
        var placements = placementRepo.findByYardIdOrderByYMetersAscXMetersAsc(yardId);
        var issues = new ArrayList<ValidationIssue>();
        var margin = 0.3; // minimum gap in meters

        for (int i = 0; i < placements.size(); i++) {
            var a = placements.get(i);
            for (int j = i + 1; j < placements.size(); j++) {
                var b = placements.get(j);
                if (rectsOverlap(a, b, margin)) {
                    issues.add(new ValidationIssue("COLLISION", a.getId(),
                            a.getBooking().getBoat().getModel(),
                            "Överlappar med " + b.getBooking().getBoat().getModel()));
                }
            }
        }
        return issues;
    }

    private boolean rectsOverlap(StoragePlacement a, StoragePlacement b, double margin) {
        double ax1 = a.getXMeters().doubleValue();
        double ay1 = a.getYMeters().doubleValue();
        double ax2 = ax1 + a.getWidthMeters().doubleValue();
        double ay2 = ay1 + a.getLengthMeters().doubleValue();

        double bx1 = b.getXMeters().doubleValue();
        double by1 = b.getYMeters().doubleValue();
        double bx2 = bx1 + b.getWidthMeters().doubleValue();
        double by2 = by1 + b.getLengthMeters().doubleValue();

        // Expand A by margin/2 on each side
        ax1 -= margin / 2; ay1 -= margin / 2;
        ax2 += margin / 2; ay2 += margin / 2;

        return ax1 < bx2 && ax2 > bx1 && ay1 < by2 && ay2 > by1;
    }

    private double estimateWidth(StorageYard yard) {
        if (yard.getBoundary() == null || yard.getBoundary().size() < 2) return 40.0;
        double maxX = yard.getBoundary().stream()
                .mapToDouble(p -> p.getXMeters().doubleValue())
                .max().orElse(40.0);
        double minX = yard.getBoundary().stream()
                .mapToDouble(p -> p.getXMeters().doubleValue())
                .min().orElse(0.0);
        return maxX - minX;
    }

    private StorageYard getOrThrow(Long id) {
        return yardRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gårdsplan " + id + " hittades inte"));
    }
}
