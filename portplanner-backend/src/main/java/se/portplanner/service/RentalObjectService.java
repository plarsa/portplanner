package se.portplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.RentalObjectRequest;
import se.portplanner.dto.RentalObjectResponse;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.RentalObject;
import se.portplanner.repository.RentalObjectRepository;

import java.util.List;

@Service
@Transactional
public class RentalObjectService {

    private final RentalObjectRepository repo;
    private final AuditService auditService;

    public RentalObjectService(RentalObjectRepository repo, AuditService auditService) {
        this.repo = repo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RentalObjectResponse> findAll() {
        return repo.findAll().stream().map(RentalObjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RentalObjectResponse> findBookable() {
        return repo.findByBookableTrueOrderByNameAsc().stream().map(RentalObjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RentalObjectResponse findById(Long id) {
        return RentalObjectResponse.from(getOrThrow(id));
    }

    public RentalObjectResponse create(RentalObjectRequest req) {
        var o = new RentalObject();
        mapFields(o, req);
        var saved = repo.save(o);
        auditService.log("CREATED", "RENTAL_OBJECT", saved.getId(), "Rental object created: " + saved.getName());
        return RentalObjectResponse.from(saved);
    }

    public RentalObjectResponse update(Long id, RentalObjectRequest req) {
        var o = getOrThrow(id);
        mapFields(o, req);
        var saved = repo.save(o);
        auditService.log("UPDATED", "RENTAL_OBJECT", saved.getId(), "Rental object updated: " + saved.getName());
        return RentalObjectResponse.from(saved);
    }

    public void delete(Long id) {
        var o = getOrThrow(id);
        repo.delete(o);
        auditService.log("DELETED", "RENTAL_OBJECT", id, "Rental object deleted: " + o.getName());
    }

    RentalObject getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental object " + id + " was not found"));
    }

    private void mapFields(RentalObject o, RentalObjectRequest req) {
        o.setName(req.name());
        o.setDescription(req.description());
        o.setBookable(req.bookable());
        o.setMaxDurationHours(req.maxDurationHours());
        o.setRequiresApproval(req.requiresApproval());
        o.setMaxDaysAhead(req.maxDaysAhead());
    }
}
