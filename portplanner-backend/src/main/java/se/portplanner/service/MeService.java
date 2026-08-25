package se.portplanner.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.*;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.*;
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

    public MeService(AppUserRepository userRepository,
                     AssignmentRepository assignmentRepository,
                     QueueEntryRepository queueRepository,
                     BoatRepository boatRepository,
                     PersonRepository personRepository) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.queueRepository = queueRepository;
        this.boatRepository = boatRepository;
        this.personRepository = personRepository;
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

    private AppUser currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Användare hittades inte"));
    }
}
