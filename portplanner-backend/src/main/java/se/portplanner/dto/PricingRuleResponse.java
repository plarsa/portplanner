package se.portplanner.dto;

import se.portplanner.model.PricingRule;

import java.math.BigDecimal;

public record PricingRuleResponse(Long id, Long seasonId,
                                  BigDecimal pricePerSqm,
                                  BigDecimal extraWidthThresholdM,
                                  BigDecimal extraWidthSurchargePerDm,
                                  BigDecimal extraLengthThresholdM,
                                  BigDecimal extraLengthSurchargePerDm,
                                  BigDecimal minPrice) {
    public static PricingRuleResponse from(PricingRule r) {
        return new PricingRuleResponse(r.getId(), r.getSeason().getId(),
                r.getPricePerSqm(),
                r.getExtraWidthThresholdM(), r.getExtraWidthSurchargePerDm(),
                r.getExtraLengthThresholdM(), r.getExtraLengthSurchargePerDm(),
                r.getMinPrice());
    }
}
