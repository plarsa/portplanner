package se.portplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A fault the borrower noted when returning the object (REN-8). It is a record, not a block: the
 * object stays bookable and the times after the return remain available.
 */
@Entity
@Table(name = "rental_fault_reports")
@Getter
@Setter
public class RentalFaultReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private RentalBooking booking;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String description;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;
}
