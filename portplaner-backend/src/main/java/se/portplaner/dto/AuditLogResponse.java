package se.portplaner.dto;

import se.portplaner.model.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        LocalDateTime occurredAt,
        String eventType,
        String entityType,
        Long entityId,
        String description,
        String performedBy
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getOccurredAt(),
                log.getEventType(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDescription(),
                log.getPerformedBy()
        );
    }
}
