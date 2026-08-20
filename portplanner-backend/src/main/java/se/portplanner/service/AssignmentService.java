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
                .orElseThrow(() -> new ResourceNotFoundException("Båt " + req.boatId() + " hittades inte"));
        var slip = slipRepository.findById(req.slipId())
                .orElseThrow(() -> new ResourceNotFoundException("Båtplats " + req.slipId() + " hittades inte"));

        validateBoatFitsSlip(boat, slip);

        if (slip.getStatus() != SlipStatus.AVAILABLE) {
            throw new IllegalArgumentException("Platsen är inte ledig (status: " + slip.getStatus() + ")");
        }
        if (assignmentRepository.findByBoatIdAndStatus(req.boatId(), AssignmentStatus.ACTIVE).isPresent()) {
            throw new IllegalArgumentException("Båten har redan en aktiv tilldelning");
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
                "Tilldelning: " + boat.getModel() + " → plats " + slip.getSlipNumber() +
                " (" + slip.getDock().getName() + ")");
        return AssignmentResponse.from(saved);
    }

    public AssignmentResponse end(Long id) {
        var assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tilldelning " + id + " hittades inte"));
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new IllegalArgumentException("Tilldelningen är redan avslutad");
        }

        assignment.setStatus(AssignmentStatus.ENDED);
        assignment.setEndDate(LocalDate.now());

        var slip = assignment.getSlip();
        slip.setStatus(SlipStatus.AVAILABLE);
        slipRepository.save(slip);

        var saved = assignmentRepository.save(assignment);
        auditService.log("UNASSIGNED", "ASSIGNMENT", id,
                "Tilldelning avslutad: " + saved.getBoat().getModel() + " lämnar plats " +
                slip.getSlipNumber() + " (" + slip.getDock().getName() + ")");
        return AssignmentResponse.from(saved);
    }

    public void validateBoatFitsSlip(Boat boat, Slip slip) {
        if (boat.getLengthM().compareTo(slip.getMaxLengthM()) > 0) {
            throw new IllegalArgumentException(
                    "Båten är för lång (%.1fm) för platsen (max %.1fm)".formatted(boat.getLengthM(), slip.getMaxLengthM()));
        }
        if (boat.getWidthM().compareTo(slip.getMaxWidthM()) > 0) {
            throw new IllegalArgumentException(
                    "Båten är för bred (%.1fm) för platsen (max %.1fm)".formatted(boat.getWidthM(), slip.getMaxWidthM()));
        }
        if (boat.getDraftM() != null && slip.getMaxDraftM() != null
                && boat.getDraftM().compareTo(slip.getMaxDraftM()) > 0) {
            throw new IllegalArgumentException(
                    "Båtens djupgång (%.1fm) överstiger platsens max (%.1fm)".formatted(boat.getDraftM(), slip.getMaxDraftM()));
        }
    }
}
