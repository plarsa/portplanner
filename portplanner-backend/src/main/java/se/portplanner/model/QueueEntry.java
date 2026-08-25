package se.portplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_entries")
@Getter
@Setter
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boat_id")
    private Boat boat;

    @Column(name = "requested_date", nullable = false)
    private LocalDateTime requestedDate = LocalDateTime.now();

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_slip_id")
    private Slip offeredSlip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueEntryStatus status = QueueEntryStatus.WAITING;
}
