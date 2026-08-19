package se.portplaner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplaner.dto.SlipRequest;
import se.portplaner.dto.SlipResponse;
import se.portplaner.exception.ResourceNotFoundException;
import se.portplaner.model.Slip;
import se.portplaner.model.SlipStatus;
import se.portplaner.repository.AssignmentRepository;
import se.portplaner.repository.DockRepository;
import se.portplaner.repository.SlipRepository;

import java.util.List;

@Service
@Transactional
public class SlipService {

    private final SlipRepository slipRepository;
    private final DockRepository dockRepository;
    private final AssignmentRepository assignmentRepository;
    private final AuditService auditService;

    public SlipService(SlipRepository slipRepository, DockRepository dockRepository,
                       AssignmentRepository assignmentRepository, AuditService auditService) {
        this.slipRepository = slipRepository;
        this.dockRepository = dockRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SlipResponse> findAll() {
        return slipRepository.findAll().stream().map(SlipResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SlipResponse findById(Long id) {
        return SlipResponse.from(getOrThrow(id));
    }

    public SlipResponse create(SlipRequest req) {
        var slip = new Slip();
        mapFields(slip, req);
        var saved = slipRepository.save(slip);
        auditService.log("CREATED", "SLIP", saved.getId(),
                "Plats skapad: " + saved.getSlipNumber() + " på brygga " + saved.getDock().getName());
        return SlipResponse.from(saved);
    }

    public SlipResponse update(Long id, SlipRequest req) {
        var slip = getOrThrow(id);
        mapFields(slip, req);
        var saved = slipRepository.save(slip);
        auditService.log("UPDATED", "SLIP", saved.getId(),
                "Plats uppdaterad: " + saved.getSlipNumber() + " på brygga " + saved.getDock().getName());
        return SlipResponse.from(saved);
    }

    public void delete(Long id) {
        Slip slip = getOrThrow(id);
        String desc = "Plats borttagen: " + slip.getSlipNumber() + " på brygga " + slip.getDock().getName();
        assignmentRepository.deleteBySlipId(id);
        slipRepository.delete(slip);
        auditService.log("DELETED", "SLIP", id, desc);
    }

    private Slip getOrThrow(Long id) {
        return slipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Båtplats " + id + " hittades inte"));
    }

    private void mapFields(Slip slip, SlipRequest req) {
        var dock = dockRepository.findById(req.dockId())
                .orElseThrow(() -> new ResourceNotFoundException("Brygga " + req.dockId() + " hittades inte"));
        slip.setSlipNumber(req.slipNumber());
        slip.setMaxLengthM(req.maxLengthM());
        slip.setMaxWidthM(req.maxWidthM());
        slip.setMaxDraftM(req.maxDraftM());
        slip.setDock(dock);
        slip.setStatus(req.status() != null ? req.status() : SlipStatus.AVAILABLE);
        slip.setCategory(req.category());
    }
}
