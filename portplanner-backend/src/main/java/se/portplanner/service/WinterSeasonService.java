package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.WinterSeasonRequest;
import se.portplanner.dto.WinterSeasonResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.WinterSeason;
import se.portplanner.model.WinterSeasonStatus;
import se.portplanner.repository.WinterSeasonRepository;

import java.util.List;

@Service
@Transactional
public class WinterSeasonService {

    private final WinterSeasonRepository repo;
    private final AuditService auditService;

    public WinterSeasonService(WinterSeasonRepository repo, AuditService auditService) {
        this.repo = repo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<WinterSeasonResponse> findAll() {
        return repo.findAllByOrderByYearDesc().stream().map(WinterSeasonResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WinterSeasonResponse findById(Long id) {
        return WinterSeasonResponse.from(getOrThrow(id));
    }

    public WinterSeasonResponse create(WinterSeasonRequest req) {
        var s = new WinterSeason();
        mapFields(s, req);
        var saved = repo.save(s);
        auditService.log("CREATED", "WINTER_SEASON", saved.getId(), "Vintersäsong skapad: " + saved.getName());
        return WinterSeasonResponse.from(saved);
    }

    public WinterSeasonResponse update(Long id, WinterSeasonRequest req) {
        var s = getOrThrow(id);
        mapFields(s, req);
        var saved = repo.save(s);
        auditService.log("UPDATED", "WINTER_SEASON", saved.getId(), "Vintersäsong uppdaterad: " + saved.getName());
        return WinterSeasonResponse.from(saved);
    }

    public void delete(Long id) {
        var s = getOrThrow(id);
        repo.delete(s);
        auditService.log("DELETED", "WINTER_SEASON", id, "Vintersäsong borttagen: " + s.getName());
    }

    private WinterSeason getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vintersäsong " + id + " hittades inte"));
    }

    private void mapFields(WinterSeason s, WinterSeasonRequest req) {
        s.setYear(req.year());
        s.setName(req.name());
        s.setStartDate(req.startDate());
        s.setEndDate(req.endDate());
        if (req.status() != null) s.setStatus(WinterSeasonStatus.valueOf(req.status()));
    }
}
