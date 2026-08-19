package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.AuditLogResponse;
import se.portplanner.repository.AuditLogRepository;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit")
@Transactional(readOnly = true)
public class AuditController {

    private final AuditLogRepository repo;

    public AuditController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @Operation(summary = "Hämta de senaste 200 händelserna, nyast först")
    public List<AuditLogResponse> findAll(
            @RequestParam(required = false) String entityType) {
        var logs = (entityType != null && !entityType.isBlank())
                ? repo.findTop200ByEntityTypeOrderByOccurredAtDesc(entityType)
                : repo.findTop200ByOrderByOccurredAtDesc();
        return logs.stream().map(AuditLogResponse::from).toList();
    }
}
