package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.HaulOutBookingRequest;
import se.portplanner.dto.HaulOutBookingResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.HaulOutBooking;
import se.portplanner.model.HaulOutBookingStatus;
import se.portplanner.repository.BoatRepository;
import se.portplanner.repository.HaulOutBookingRepository;
import se.portplanner.repository.HaulOutSlotRepository;
import se.portplanner.repository.PersonRepository;

import java.util.List;

@Service
@Transactional
public class HaulOutBookingService {

    private final HaulOutBookingRepository bookingRepo;
    private final HaulOutSlotRepository slotRepo;
    private final BoatRepository boatRepo;
    private final PersonRepository personRepo;
    private final AuditService auditService;

    public HaulOutBookingService(HaulOutBookingRepository bookingRepo,
                                  HaulOutSlotRepository slotRepo,
                                  BoatRepository boatRepo,
                                  PersonRepository personRepo,
                                  AuditService auditService) {
        this.bookingRepo = bookingRepo;
        this.slotRepo = slotRepo;
        this.boatRepo = boatRepo;
        this.personRepo = personRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<HaulOutBookingResponse> findBySlot(Long slotId) {
        return bookingRepo.findBySlotIdOrderByRequestedAtAsc(slotId).stream()
                .map(HaulOutBookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HaulOutBookingResponse> findBySeason(Long seasonId) {
        return bookingRepo.findBySlotSeasonIdAndStatusIn(seasonId,
                List.of(HaulOutBookingStatus.REQUESTED, HaulOutBookingStatus.CONFIRMED,
                        HaulOutBookingStatus.COMPLETED)).stream()
                .map(HaulOutBookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public HaulOutBookingResponse findById(Long id) {
        return HaulOutBookingResponse.from(getOrThrow(id));
    }

    public HaulOutBookingResponse create(HaulOutBookingRequest req) {
        var slot = slotRepo.findById(req.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Upptagningstid " + req.slotId() + " hittades inte"));
        int booked = bookingRepo.countBySlotIdAndStatusNot(slot.getId(), HaulOutBookingStatus.CANCELLED);
        if (booked >= slot.getCapacity()) {
            throw new IllegalStateException("This slot is fully booked");
        }
        var boat = boatRepo.findById(req.boatId())
                .orElseThrow(() -> new ResourceNotFoundException("Boat " + req.boatId() + " was not found"));
        var person = personRepo.findById(req.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person " + req.personId() + " hittades inte"));

        var booking = new HaulOutBooking();
        booking.setSlot(slot);
        booking.setBoat(boat);
        booking.setPerson(person);
        var saved = bookingRepo.save(booking);
        auditService.log("CREATED", "HAUL_OUT_BOOKING", saved.getId(),
                "Booking created: " + boat.getModel() + " for " + slot.getSlotDate());
        return HaulOutBookingResponse.from(saved);
    }

    public HaulOutBookingResponse confirm(Long id) {
        var booking = getOrThrow(id);
        booking.setStatus(HaulOutBookingStatus.CONFIRMED);
        auditService.log("CONFIRMED", "HAUL_OUT_BOOKING", id, "Booking confirmed: " + booking.getBoat().getModel());
        return HaulOutBookingResponse.from(bookingRepo.save(booking));
    }

    public void delete(Long id) {
        var booking = getOrThrow(id);
        booking.setStatus(HaulOutBookingStatus.CANCELLED);
        bookingRepo.save(booking);
        auditService.log("CANCELLED", "HAUL_OUT_BOOKING", id, "Booking cancelled: " + booking.getBoat().getModel());
    }

    private HaulOutBooking getOrThrow(Long id) {
        return bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + id + " was not found"));
    }
}
