package se.portplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "storage_yards")
@Getter
@Setter
public class StorageYard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private WinterSeason season;

    @Column(nullable = false)
    private String name;

    @Column(name = "background_image_url", length = 1024)
    private String backgroundImageUrl;

    @Column(name = "origin_pixel_x")
    private Double originPixelX;

    @Column(name = "origin_pixel_y")
    private Double originPixelY;

    @Column(name = "pixels_per_meter")
    private Double pixelsPerMeter;

    @Column(name = "lane_margin_m", precision = 5, scale = 2)
    private BigDecimal laneMarginM = new BigDecimal("0.50");

    @OneToMany(mappedBy = "yard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("pointOrder ASC")
    private List<StorageYardBoundaryPoint> boundary = new ArrayList<>();
}
