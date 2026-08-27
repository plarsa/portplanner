package se.portplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private WinterSeason season;

    @Column(name = "price_per_sqm", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSqm;

    @Column(name = "extra_width_threshold_m", precision = 6, scale = 2)
    private BigDecimal extraWidthThresholdM;

    @Column(name = "extra_width_surcharge_per_dm", precision = 10, scale = 2)
    private BigDecimal extraWidthSurchargePerDm;

    @Column(name = "extra_length_threshold_m", precision = 6, scale = 2)
    private BigDecimal extraLengthThresholdM;

    @Column(name = "extra_length_surcharge_per_dm", precision = 10, scale = 2)
    private BigDecimal extraLengthSurchargePerDm;

    @Column(name = "min_price", precision = 10, scale = 2)
    private BigDecimal minPrice;
}
