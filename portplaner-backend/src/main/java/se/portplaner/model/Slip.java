package se.portplaner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "slips", uniqueConstraints = @UniqueConstraint(columnNames = {"dock_id", "slip_number"}))
@Getter
@Setter
public class Slip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slip_number", nullable = false)
    private String slipNumber;

    @Column(name = "max_length_m", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxLengthM;

    @Column(name = "max_width_m", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxWidthM;

    @Column(name = "max_draft_m", precision = 6, scale = 2)
    private BigDecimal maxDraftM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dock_id", nullable = false)
    private Dock dock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlipStatus status = SlipStatus.AVAILABLE;
}
