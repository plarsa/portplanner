package se.portplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import se.portplanner.file.StoredFile;

/** Something the club rents out, together with the rules a booking is checked against (REN-1, REN-3). */
@Entity
@Table(name = "rental_objects")
@Getter
@Setter
public class RentalObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "CLOB")
    private String description;

    /** False takes the object out of the calendar without deleting it or its history. */
    @Column(nullable = false)
    private boolean bookable = true;

    @Column(name = "max_duration_hours", nullable = false)
    private int maxDurationHours = 24;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval = false;

    @Column(name = "max_days_ahead", nullable = false)
    private int maxDaysAhead = 30;

    /** Served only through the module's own API, never from a public path (REN-2). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_file_id")
    private StoredFile image;
}
