package se.portplaner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.portplaner.model.AuditLog;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByOrderByOccurredAtDesc();
    List<AuditLog> findTop200ByEntityTypeOrderByOccurredAtDesc(String entityType);
}
