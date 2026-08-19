package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.DockRequest;
import se.portplanner.dto.DockResponse;
import se.portplanner.dto.SlipResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.Dock;
import se.portplanner.model.Slip;
import se.portplanner.repository.AssignmentRepository;
import se.portplanner.repository.DockRepository;
import se.portplanner.repository.SlipRepository;

import java.util.List;

@Service
@Transactional
public class DockService {

    private final DockRepository dockRepository;
    private final SlipRepository slipRepository;
    private final AssignmentRepository assignmentRepository;
    private final AuditService auditService;

    public DockService(DockRepository dockRepository, SlipRepository slipRepository,
                       AssignmentRepository assignmentRepository, AuditService auditService) {
        this.dockRepository = dockRepository;
        this.slipRepository = slipRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DockResponse> findAll() {
        return dockRepository.findAll().stream().map(DockResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DockResponse findById(Long id) {
        return DockResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<SlipResponse> findSlips(Long dockId) {
        getOrThrow(dockId);
        return slipRepository.findByDockId(dockId).stream().map(SlipResponse::from).toList();
    }

    public DockResponse create(DockRequest req) {
        var dock = new Dock();
        dock.setName(req.name());
        dock.setDescription(req.description());
        var saved = dockRepository.save(dock);
        auditService.log("CREATED", "DOCK", saved.getId(), "Brygga skapad: " + saved.getName());
        return DockResponse.from(saved);
    }

    public DockResponse update(Long id, DockRequest req) {
        var dock = getOrThrow(id);
        dock.setName(req.name());
        dock.setDescription(req.description());
        var saved = dockRepository.save(dock);
        auditService.log("UPDATED", "DOCK", saved.getId(), "Brygga uppdaterad: " + saved.getName());
        return DockResponse.from(saved);
    }

    public void delete(Long id) {
        Dock dock = getOrThrow(id);
        List<Slip> slips = slipRepository.findByDockId(id);
        for (Slip slip : slips) {
            assignmentRepository.deleteBySlipId(slip.getId());
        }
        slipRepository.deleteAll(slips);
        dockRepository.deleteById(id);
        auditService.log("DELETED", "DOCK", id,
                "Brygga borttagen: " + dock.getName() + " (" + slips.size() + " platser)");
    }

    private Dock getOrThrow(Long id) {
        return dockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brygga " + id + " hittades inte"));
    }
}
