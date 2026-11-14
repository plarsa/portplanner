package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.AssignmentRequest;
import se.portplanner.dto.AssignmentResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.*;
import se.portplanner.repository.AssignmentRepository;
import se.portplanner.repository.BoatRepository;
import se.portplanner.repository.SlipRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final BoatRepository boatRepository;
    private final SlipRepository slipRepository;
    private final AuditService auditService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             BoatRepository boatRepository,
                             SlipRepository slipRepository,
                             AuditService auditService) {
        this.assignmentRepository = assignmentRepository;
        this.boatRepository = boatRepository;
        this.slipRepository = slipRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> findActive() {
        return assignmentRepository.findByStatus(AssignmentStatus.ACTIVE)
                .stream().map(AssignmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<AssignmentResponse> findActiveBySlip(Long slipId) {
        return assignmentRepository.findBySlipIdAndStatus(slipId, AssignmentStatus.ACTIVE)
                .map(AssignmentResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> findByBoat(Long boatId) {
        return assignmentRepository.findByBoatId(boatId)
                .stream().map(AssignmentResponse::from).toList();
    }

    public AssignmentResponse create(AssignmentRequest req) {
        var boat = boatRepository.findById(req.boatId())
                .orElseThrow(() -> new ResourceNotFoundException("Boat " + req.boatId() + " was not found"));
        var slip = slipRepository.findById(req.slipId())
                .orElseThrow(() -> new ResourceNotFoundException("Slip " + req.slipId() + " was not found"));

        validateBoatFitsSlip(boat, slip);

        if (slip.getStatus() != SlipStatus.AVAILABLE) {
            throw new IllegalArgumentException("The slip is not available (status: " + slip.getStatus() + ")");
        }
        if (assignmentRepository.findByBoatIdAndStatus(req.boatId(), AssignmentStatus.ACTIVE).isPresent()) {
            throw new IllegalArgumentException("The boat already has an active assignment");
        }

        slip.setStatus(SlipStatus.OCCUPIED);
        slipRepository.save(slip);

        var assignment = new Assignment();
        assignment.setBoat(boat);
        assignment.setSlip(slip);
        assignment.setAssignedDate(LocalDate.now());
        assignment.setStatus(AssignmentStatus.ACTIVE);

        var saved = assignmentRepository.save(assignment);
        auditService.log("ASSIGNED", "ASSIGNMENT", saved.getId(),
                "Assigned: " + boat.getModel() + " → slip " + slip.getSlipNumber() +
                " (" + slip.getDock().getName() + ")");
        return AssignmentResponse.from(saved);
    }

    public AssignmentResponse end(Long id) {
        var assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment " + id + " was not found"));
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new IllegalArgumentException("The assignment has already ended");
        }

        assignment.setStatus(AssignmentStatus.ENDED);
        assignment.setEndDate(LocalDate.now());

        var slip = assignment.getSlip();
        slip.setStatus(SlipStatus.AVAILABLE);
        slipRepository.save(slip);

        var saved = assignmentRepository.save(assignment);
        auditService.log("UNASSIGNED", "ASSIGNMENT", id,
                "Assignment ended: " + saved.getBoat().getModel() + " leaves slip " +
                slip.getSlipNumber() + " (" + slip.getDock().getName() + ")");
        return AssignmentResponse.from(saved);
    }

    public void validateBoatFitsSlip(Boat boat, Slip slip) {
        if (boat.getLengthM().compareTo(slip.getMaxLengthM()) > 0) {
            throw new IllegalArgumentException(
                    "The boat is too long (%.1fm) for the slip (max %.1fm)".formatted(boat.getLengthM(), slip.getMaxLengthM()));
        }
        if (boat.getWidthM().compareTo(slip.getMaxWidthM()) > 0) {
            throw new IllegalArgumentException(
                    "The boat is too wide (%.1fm) for the slip (max %.1fm)".formatted(boat.getWidthM(), slip.getMaxWidthM()));
        }
        if (boat.getDraftM() != null && slip.getMaxDraftM() != null
                && boat.getDraftM().compareTo(slip.getMaxDraftM()) > 0) {
            throw new IllegalArgumentException(
                    "The boat's draft (%.1fm) exceeds the slip maximum (%.1fm)".formatted(boat.getDraftM(), slip.getMaxDraftM()));
        }
    }
}
