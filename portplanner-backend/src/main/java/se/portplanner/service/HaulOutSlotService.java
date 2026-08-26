package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.HaulOutSlotRequest;
import se.portplanner.dto.HaulOutSlotResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.HaulOutBookingStatus;
import se.portplanner.model.HaulOutSlot;
import se.portplanner.repository.HaulOutBookingRepository;
import se.portplanner.repository.HaulOutSlotRepository;
import se.portplanner.repository.WinterSeasonRepository;

import java.util.List;

@Service
@Transactional
public class HaulOutSlotService {

    private final HaulOutSlotRepository slotRepo;
    private final WinterSeasonRepository seasonRepo;
    private final HaulOutBookingRepository bookingRepo;
    private final AuditService auditService;

    public HaulOutSlotService(HaulOutSlotRepository slotRepo, WinterSeasonRepository seasonRepo,
                               HaulOutBookingRepository bookingRepo, AuditService auditService) {
        this.slotRepo = slotRepo;
        this.seasonRepo = seasonRepo;
        this.bookingRepo = bookingRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<HaulOutSlotResponse> findBySeason(Long seasonId) {
        return slotRepo.findBySeasonIdOrderBySlotDateAscStartTimeAsc(seasonId).stream()
                .map(s -> HaulOutSlotResponse.from(s,
                        bookingRepo.countBySlotIdAndStatusNot(s.getId(), HaulOutBookingStatus.CANCELLED)))
                .toList();
    }

    @Transactional(readOnly = true)
    public HaulOutSlotResponse findById(Long id) {
        var s = getOrThrow(id);
        return HaulOutSlotResponse.from(s,
                bookingRepo.countBySlotIdAndStatusNot(s.getId(), HaulOutBookingStatus.CANCELLED));
    }

    public HaulOutSlotResponse create(HaulOutSlotRequest req) {
        var season = seasonRepo.findById(req.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Vintersäsong " + req.seasonId() + " hittades inte"));
        var slot = new HaulOutSlot();
        slot.setSeason(season);
        slot.setSlotDate(req.slotDate());
        slot.setStartTime(req.startTime());
        slot.setEndTime(req.endTime());
        slot.setCapacity(req.capacity() != null ? req.capacity() : 1);
        var saved = slotRepo.save(slot);
        auditService.log("CREATED", "HAUL_OUT_SLOT", saved.getId(), "Upptagningstid skapad: " + saved.getSlotDate());
        return HaulOutSlotResponse.from(saved, 0);
    }

    public HaulOutSlotResponse update(Long id, HaulOutSlotRequest req) {
        var slot = getOrThrow(id);
        slot.setSlotDate(req.slotDate());
        slot.setStartTime(req.startTime());
        slot.setEndTime(req.endTime());
        if (req.capacity() != null) slot.setCapacity(req.capacity());
        var saved = slotRepo.save(slot);
        int booked = bookingRepo.countBySlotIdAndStatusNot(saved.getId(), HaulOutBookingStatus.CANCELLED);
        return HaulOutSlotResponse.from(saved, booked);
    }

    public void delete(Long id) {
        var slot = getOrThrow(id);
        slotRepo.delete(slot);
        auditService.log("DELETED", "HAUL_OUT_SLOT", id, "Upptagningstid borttagen: " + slot.getSlotDate());
    }

    private HaulOutSlot getOrThrow(Long id) {
        return slotRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Upptagningstid " + id + " hittades inte"));
    }
}
