package se.portplaner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplaner.dto.AssignmentRequest;
import se.portplaner.dto.AssignmentResponse;
import se.portplaner.dto.QueueEntryRequest;
import se.portplaner.dto.QueueEntryResponse;
import se.portplaner.dto.SlipResponse;
import se.portplaner.exception.ResourceNotFoundException;
import se.portplaner.model.*;
import se.portplaner.repository.*;

import java.util.List;

@Service
@Transactional
public class QueueService {

    private final QueueEntryRepository queueRepository;
    private final PersonRepository personRepository;
    private final BoatRepository boatRepository;
    private final SlipRepository slipRepository;
    private final AssignmentService assignmentService;

    public QueueService(QueueEntryRepository queueRepository,
                        PersonRepository personRepository,
                        BoatRepository boatRepository,
                        SlipRepository slipRepository,
                        AssignmentService assignmentService) {
        this.queueRepository = queueRepository;
        this.personRepository = personRepository;
        this.boatRepository = boatRepository;
        this.slipRepository = slipRepository;
        this.assignmentService = assignmentService;
    }

    @Transactional(readOnly = true)
    public List<QueueEntryResponse> findWaiting() {
        return queueRepository.findByStatusOrderByRequestedDateAsc(QueueEntryStatus.WAITING)
                .stream().map(QueueEntryResponse::from).toList();
    }

    public QueueEntryResponse add(QueueEntryRequest req) {
        var person = personRepository.findById(req.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person " + req.personId() + " hittades inte"));
        var boat = boatRepository.findById(req.boatId())
                .orElseThrow(() -> new ResourceNotFoundException("Båt " + req.boatId() + " hittades inte"));

        var entry = new QueueEntry();
        entry.setPerson(person);
        entry.setBoat(boat);
        entry.setNotes(req.notes());
        entry.setStatus(QueueEntryStatus.WAITING);

        return QueueEntryResponse.from(queueRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<SlipResponse> suggestions(Long entryId) {
        var entry = getOrThrow(entryId);
        var boat = entry.getBoat();

        return slipRepository.findByStatus(SlipStatus.AVAILABLE).stream()
                .filter(slip -> boat.getLengthM().compareTo(slip.getMaxLengthM()) <= 0)
                .filter(slip -> boat.getWidthM().compareTo(slip.getMaxWidthM()) <= 0)
                .filter(slip -> boat.getDraftM() == null || slip.getMaxDraftM() == null
                        || boat.getDraftM().compareTo(slip.getMaxDraftM()) <= 0)
                .map(SlipResponse::from)
                .toList();
    }

    public AssignmentResponse assignFromQueue(Long entryId, Long slipId) {
        var entry = getOrThrow(entryId);
        if (entry.getStatus() != QueueEntryStatus.WAITING) {
            throw new IllegalArgumentException("Kö-posten är inte i vänteläge");
        }

        var assignment = assignmentService.create(new AssignmentRequest(entry.getBoat().getId(), slipId));

        entry.setStatus(QueueEntryStatus.ASSIGNED);
        queueRepository.save(entry);

        return assignment;
    }

    public void cancel(Long id) {
        var entry = getOrThrow(id);
        entry.setStatus(QueueEntryStatus.CANCELLED);
        queueRepository.save(entry);
    }

    private QueueEntry getOrThrow(Long id) {
        return queueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kö-post " + id + " hittades inte"));
    }
}
