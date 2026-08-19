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

    public SlipService(SlipRepository slipRepository, DockRepository dockRepository,
                       AssignmentRepository assignmentRepository) {
        this.slipRepository = slipRepository;
        this.dockRepository = dockRepository;
        this.assignmentRepository = assignmentRepository;
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
        return SlipResponse.from(slipRepository.save(slip));
    }

    public SlipResponse update(Long id, SlipRequest req) {
        var slip = getOrThrow(id);
        mapFields(slip, req);
        return SlipResponse.from(slipRepository.save(slip));
    }

    public void delete(Long id) {
        Slip slip = getOrThrow(id);
        assignmentRepository.deleteBySlipId(id);
        slipRepository.delete(slip);
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
    }
}
