package se.portplanner.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.*;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.*;
import se.portplanner.model.HaulOutBooking;
import se.portplanner.model.HaulOutBookingStatus;
import se.portplanner.repository.*;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MeService {

    private final AppUserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final QueueEntryRepository queueRepository;
    private final BoatRepository boatRepository;
    private final PersonRepository personRepository;
    private final HaulOutSlotRepository haulOutSlotRepository;
    private final HaulOutBookingRepository haulOutBookingRepository;

    public MeService(AppUserRepository userRepository,
                     AssignmentRepository assignmentRepository,
                     QueueEntryRepository queueRepository,
                     BoatRepository boatRepository,
                     PersonRepository personRepository,
                     HaulOutSlotRepository haulOutSlotRepository,
                     HaulOutBookingRepository haulOutBookingRepository) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.queueRepository = queueRepository;
        this.boatRepository = boatRepository;
        this.personRepository = personRepository;
        this.haulOutSlotRepository = haulOutSlotRepository;
        this.haulOutBookingRepository = haulOutBookingRepository;
    }

    public MeResponse getMe() {
        var user = currentUser();
        if (user.getPerson() == null) throw new IllegalStateException("Ditt konto är inte kopplat till en person");
        return MeResponse.from(user.getPerson());
    }

    public List<BoatResponse> getMyBoats() {
        var person = currentUser().getPerson();
        if (person == null) throw new IllegalStateException("Ditt konto är inte kopplat till en person");
        return boatRepository.findByOwnerId(person.getId()).stream()
                .map(BoatResponse::from).toList();
    }

    public List<AssignmentResponse> getMyAssignments() {
        var person = currentUser().getPerson();
        if (person == null) throw new IllegalStateException("Ditt konto är inte kopplat till en person");
        return assignmentRepository.findByBoatOwnerIdAndStatus(person.getId(), AssignmentStatus.ACTIVE)
                .stream().map(AssignmentResponse::from).toList();
    }

    public List<QueueEntryResponse> getMyQueue() {
        var person = currentUser().getPerson();
        if (person == null) throw new IllegalStateException("Ditt konto är inte kopplat till en person");
        return queueRepository.findByPersonId(person.getId()).stream()
                .filter(e -> e.getStatus() == QueueEntryStatus.WAITING || e.getStatus() == QueueEntryStatus.OFFERED)
                .map(QueueEntryResponse::from).toList();
    }

    @Transactional
    public MeResponse updateProfile(PersonRequest req) {
        var person = currentUser().getPerson();
        if (person == null) throw new IllegalStateException("Ditt konto är inte kopplat till en person");
        person.setPhone(req.phone());
        person.setAddress(req.address());
        person.setPostalCode(req.postalCode());
        personRepository.save(person);
        return MeResponse.from(person);
    }

    public List<HaulOutSlotResponse> getAvailableHaulOutSlots() {
        // Return slots with remaining capacity from ACTIVE seasons
        return haulOutSlotRepository.findAll().stream()
                .filter(s -> s.getSeason().getStatus() == se.portplanner.model.WinterSeasonStatus.ACTIVE)
                .filter(s -> haulOutBookingRepository.countBySlotIdAndStatusNot(
                        s.getId(), HaulOutBookingStatus.CANCELLED) < s.getCapacity())
                .map(s -> HaulOutSlotResponse.from(s,
                        haulOutBookingRepository.countBySlotIdAndStatusNot(
                                s.getId(), HaulOutBookingStatus.CANCELLED)))
                .toList();
    }

    public List<HaulOutBookingResponse> getMyHaulOutBookings() {
        var person = currentUser().getPerson();
        if (person == null) throw new IllegalStateException("Ditt konto är inte kopplat till en person");
        return haulOutBookingRepository.findAll().stream()
                .filter(b -> b.getPerson().getId().equals(person.getId()))
                .filter(b -> b.getStatus() != HaulOutBookingStatus.CANCELLED)
                .map(HaulOutBookingResponse::from).toList();
    }

    @Transactional
    public HaulOutBookingResponse createMyHaulOutBooking(Long slotId, Long boatId) {
        var person = currentUser().getPerson();
        if (person == null) throw new IllegalStateException("Ditt konto är inte kopplat till en person");
        var slot = haulOutSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Tid " + slotId + " hittades inte"));
        int booked = haulOutBookingRepository.countBySlotIdAndStatusNot(slot.getId(), HaulOutBookingStatus.CANCELLED);
        if (booked >= slot.getCapacity()) throw new IllegalStateException("Denna tid är fullbokad");
        var boat = boatRepository.findById(boatId)
                .orElseThrow(() -> new ResourceNotFoundException("Båt " + boatId + " hittades inte"));
        if (!boat.getOwner().getId().equals(person.getId()))
            throw new IllegalStateException("Båten tillhör inte ditt konto");
        var booking = new HaulOutBooking();
        booking.setSlot(slot);
        booking.setBoat(boat);
        booking.setPerson(person);
        return HaulOutBookingResponse.from(haulOutBookingRepository.save(booking));
    }

    private AppUser currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Användare hittades inte"));
    }
}
