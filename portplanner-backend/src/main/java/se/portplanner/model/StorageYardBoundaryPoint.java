package se.portplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "storage_yard_boundary_points")
@Getter
@Setter
public class StorageYardBoundaryPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "yard_id", nullable = false)
    private StorageYard yard;

    @Column(name = "point_order", nullable = false)
    private Integer pointOrder;

    @Column(name = "x_meters", nullable = false, precision = 10, scale = 3)
    private BigDecimal xMeters;

    @Column(name = "y_meters", nullable = false, precision = 10, scale = 3)
    private BigDecimal yMeters;
}
