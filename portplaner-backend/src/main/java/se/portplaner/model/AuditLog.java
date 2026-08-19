package se.portplaner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime occurredAt;
    private String eventType;
    private String entityType;
    private Long entityId;

    @Column(length = 500)
    private String description;

    private String performedBy;
}
