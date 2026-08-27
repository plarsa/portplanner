package se.portplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "storage_placements")
@Getter
@Setter
public class StoragePlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yard_id", nullable = false)
    private StorageYard yard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private HaulOutBooking booking;

    @Column(name = "x_meters", nullable = false, precision = 10, scale = 3)
    private BigDecimal xMeters = BigDecimal.ZERO;

    @Column(name = "y_meters", nullable = false, precision = 10, scale = 3)
    private BigDecimal yMeters = BigDecimal.ZERO;

    @Column(name = "rotation_deg", nullable = false, precision = 7, scale = 3)
    private BigDecimal rotationDeg = BigDecimal.ZERO;

    @Column(name = "width_meters", nullable = false, precision = 6, scale = 2)
    private BigDecimal widthMeters;

    @Column(name = "length_meters", nullable = false, precision = 6, scale = 2)
    private BigDecimal lengthMeters;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packing_group_id")
    private StoragePackingGroup packingGroup;

    @Column(name = "order_in_group")
    private Integer orderInGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoragePlacementStatus status = StoragePlacementStatus.PLANNED;
}
