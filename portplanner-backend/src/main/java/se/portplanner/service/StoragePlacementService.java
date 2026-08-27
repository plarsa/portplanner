package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.StoragePlacementResponse;
import se.portplanner.dto.StoragePlacementRequest;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.StoragePlacement;
import se.portplanner.model.StoragePlacementStatus;
import se.portplanner.repository.StoragePackingGroupRepository;
import se.portplanner.repository.StoragePlacementRepository;

import java.util.List;

@Service
@Transactional
public class StoragePlacementService {

    private final StoragePlacementRepository placementRepo;
    private final StoragePackingGroupRepository groupRepo;
    private final AuditService auditService;

    public StoragePlacementService(StoragePlacementRepository placementRepo,
                                    StoragePackingGroupRepository groupRepo,
                                    AuditService auditService) {
        this.placementRepo = placementRepo;
        this.groupRepo = groupRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<StoragePlacementResponse> findByYard(Long yardId) {
        return placementRepo.findByYardIdOrderByYMetersAscXMetersAsc(yardId).stream()
                .map(StoragePlacementResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StoragePlacementResponse findById(Long id) {
        return StoragePlacementResponse.from(getOrThrow(id));
    }

    public StoragePlacementResponse update(Long id, StoragePlacementRequest req) {
        var p = getOrThrow(id);
        if (req.xMeters() != null) p.setXMeters(req.xMeters());
        if (req.yMeters() != null) p.setYMeters(req.yMeters());
        if (req.rotationDeg() != null) p.setRotationDeg(req.rotationDeg());
        if (req.widthMeters() != null) p.setWidthMeters(req.widthMeters());
        if (req.lengthMeters() != null) p.setLengthMeters(req.lengthMeters());
        if (req.orderInGroup() != null) p.setOrderInGroup(req.orderInGroup());
        if (req.status() != null) p.setStatus(StoragePlacementStatus.valueOf(req.status()));
        if (req.packingGroupId() != null) {
            var group = groupRepo.findById(req.packingGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Packningsgrupp " + req.packingGroupId() + " hittades inte"));
            p.setPackingGroup(group);
        } else if (req.packingGroupId() == null && req.orderInGroup() == null) {
            // explicit null to clear group
        }
        return StoragePlacementResponse.from(placementRepo.save(p));
    }

    public StoragePlacementResponse launch(Long id) {
        var p = getOrThrow(id);
        // Check packing group order conflict
        if (p.getPackingGroup() != null && p.getOrderInGroup() != null) {
            var groupPlacements = placementRepo.findByYardIdOrderByYMetersAscXMetersAsc(p.getYard().getId())
                    .stream()
                    .filter(pp -> pp.getPackingGroup() != null
                            && pp.getPackingGroup().getId().equals(p.getPackingGroup().getId())
                            && pp.getStatus() != StoragePlacementStatus.LAUNCHED)
                    .toList();
            boolean outOfOrder = groupPlacements.stream()
                    .anyMatch(pp -> pp.getOrderInGroup() != null && pp.getOrderInGroup() < p.getOrderInGroup());
            if (outOfOrder) {
                throw new IllegalStateException(
                        "Ordningskonflikt: det finns båtar med lägre ordningsnummer som inte sjösatts i gruppen " +
                        p.getPackingGroup().getName());
            }
        }
        p.setStatus(StoragePlacementStatus.LAUNCHED);
        auditService.log("LAUNCHED", "STORAGE_PLACEMENT", id,
                "Båt sjösatt: " + p.getBooking().getBoat().getModel());
        return StoragePlacementResponse.from(placementRepo.save(p));
    }

    private StoragePlacement getOrThrow(Long id) {
        return placementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placering " + id + " hittades inte"));
    }
}
