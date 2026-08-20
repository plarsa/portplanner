package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.AssignmentRequest;
import se.portplanner.dto.AssignmentResponse;
import se.portplanner.dto.QueueEntryRequest;
import se.portplanner.dto.QueueEntryResponse;
import se.portplanner.dto.SlipResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.*;
import se.portplanner.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class QueueService {

    private final QueueEntryRepository queueRepository;
    private final PersonRepository personRepository;
    private final BoatRepository boatRepository;
    private final SlipRepository slipRepository;
    private final AssignmentService assignmentService;
    private final AuditService auditService;

    public QueueService(QueueEntryRepository queueRepository,
                        PersonRepository personRepository,
                        BoatRepository boatRepository,
                        SlipRepository slipRepository,
                        AssignmentService assignmentService,
                        AuditService auditService) {
        this.queueRepository = queueRepository;
        this.personRepository = personRepository;
        this.boatRepository = boatRepository;
        this.slipRepository = slipRepository;
        this.assignmentService = assignmentService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<QueueEntryResponse> findWaiting() {
        return queueRepository.findByStatusOrderByRequestedDateAsc(QueueEntryStatus.WAITING)
                .stream().map(QueueEntryResponse::from).toList();
    }

    public QueueEntryResponse add(QueueEntryRequest req) {
        var person = personRepository.findById(req.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person " + req.personId() + " hittades inte"));

        Boat boat = null;
        if (req.boatId() != null) {
            boat = boatRepository.findById(req.boatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Båt " + req.boatId() + " hittades inte"));
        }

        var entry = new QueueEntry();
        entry.setPerson(person);
        entry.setBoat(boat);
        entry.setNotes(req.notes());
        entry.setRequestedDate(req.requestedDate() != null ? req.requestedDate().atStartOfDay() : LocalDateTime.now());
        entry.setStatus(QueueEntryStatus.WAITING);

        var saved = queueRepository.save(entry);
        String boatPart = boat != null ? boat.getModel() + " – " : "";
        auditService.log("QUEUED", "QUEUE_ENTRY", saved.getId(),
                "Köpost tillagd: " + boatPart +
                person.getFirstName() + " " + person.getLastName());
        return QueueEntryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SlipResponse> suggestions(Long entryId) {
        var entry = getOrThrow(entryId);
        var boat = entry.getBoat();
        if (boat == null) return List.of();

        return slipRepository.findByStatus(SlipStatus.AVAILABLE).stream()
                .filter(slip -> boat.getLengthM() == null || slip.getMaxLengthM() == null
                        || boat.getLengthM().compareTo(slip.getMaxLengthM()) <= 0)
                .filter(slip -> boat.getWidthM().compareTo(slip.getMaxWidthM()) <= 0)
                .filter(slip -> boat.getDraftM() == null || slip.getMaxDraftM() == null
                        || boat.getDraftM().compareTo(slip.getMaxDraftM()) <= 0)
                .map(SlipResponse::from)
                .toList();
    }

    public QueueEntryResponse update(Long id, QueueEntryRequest req) {
        var entry = getOrThrow(id);

        Boat boat = null;
        if (req.boatId() != null) {
            boat = boatRepository.findById(req.boatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Båt " + req.boatId() + " hittades inte"));
        }
        entry.setBoat(boat);
        entry.setNotes(req.notes());
        if (req.requestedDate() != null) {
            entry.setRequestedDate(req.requestedDate().atStartOfDay());
        }
        var saved = queueRepository.save(entry);
        auditService.log("UPDATED", "QUEUE_ENTRY", id,
                "Köpost uppdaterad: " + entry.getPerson().getFirstName() + " " + entry.getPerson().getLastName());
        return QueueEntryResponse.from(saved);
    }

    public AssignmentResponse assignFromQueue(Long entryId, Long slipId) {
        var entry = getOrThrow(entryId);
        if (entry.getStatus() != QueueEntryStatus.WAITING) {
            throw new IllegalArgumentException("Kö-posten är inte i vänteläge");
        }

        var assignment = assignmentService.create(new AssignmentRequest(entry.getBoat().getId(), slipId));

        entry.setStatus(QueueEntryStatus.ASSIGNED);
        queueRepository.save(entry);

        String boatPart = entry.getBoat() != null ? entry.getBoat().getModel() + " – " : "";
        auditService.log("QUEUE_ASSIGNED", "QUEUE_ENTRY", entryId,
                "Från kö: " + boatPart +
                entry.getPerson().getFirstName() + " " + entry.getPerson().getLastName() + " tilldelad plats");
        return assignment;
    }

    public void cancel(Long id) {
        var entry = getOrThrow(id);
        String boatPart = entry.getBoat() != null ? entry.getBoat().getModel() + " – " : "";
        String desc = "Köpost avbruten: " + boatPart +
                entry.getPerson().getFirstName() + " " + entry.getPerson().getLastName();
        entry.setStatus(QueueEntryStatus.CANCELLED);
        queueRepository.save(entry);
        auditService.log("DEQUEUED", "QUEUE_ENTRY", id, desc);
    }

    private QueueEntry getOrThrow(Long id) {
        return queueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kö-post " + id + " hittades inte"));
    }
}
