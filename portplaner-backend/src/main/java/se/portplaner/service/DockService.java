package se.portplaner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplaner.dto.DockRequest;
import se.portplaner.dto.DockResponse;
import se.portplaner.dto.SlipResponse;
import se.portplaner.exception.ResourceNotFoundException;
import se.portplaner.model.Dock;
import se.portplaner.repository.DockRepository;
import se.portplaner.repository.SlipRepository;

import java.util.List;

@Service
@Transactional
public class DockService {

    private final DockRepository dockRepository;
    private final SlipRepository slipRepository;

    public DockService(DockRepository dockRepository, SlipRepository slipRepository) {
        this.dockRepository = dockRepository;
        this.slipRepository = slipRepository;
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
        return DockResponse.from(dockRepository.save(dock));
    }

    public DockResponse update(Long id, DockRequest req) {
        var dock = getOrThrow(id);
        dock.setName(req.name());
        dock.setDescription(req.description());
        return DockResponse.from(dockRepository.save(dock));
    }

    public void delete(Long id) {
        dockRepository.delete(getOrThrow(id));
    }

    private Dock getOrThrow(Long id) {
        return dockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brygga " + id + " hittades inte"));
    }
}
