package se.portplanner.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PricingRuleRequest(
        @NotNull Long seasonId,
        @NotNull @Positive BigDecimal pricePerSqm,
        BigDecimal extraWidthThresholdM,
        BigDecimal extraWidthSurchargePerDm,
        BigDecimal extraLengthThresholdM,
        BigDecimal extraLengthSurchargePerDm,
        BigDecimal minPrice
) {}
