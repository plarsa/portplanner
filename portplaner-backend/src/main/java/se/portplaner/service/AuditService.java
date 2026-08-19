package se.portplaner.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplaner.model.AuditLog;
import se.portplaner.repository.AuditLogRepository;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void log(String eventType, String entityType, Long entityId, String description) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "system";

        var entry = new AuditLog();
        entry.setOccurredAt(LocalDateTime.now());
        entry.setEventType(eventType);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDescription(description.length() > 500 ? description.substring(0, 497) + "…" : description);
        entry.setPerformedBy(user);
        repo.save(entry);
    }
}
