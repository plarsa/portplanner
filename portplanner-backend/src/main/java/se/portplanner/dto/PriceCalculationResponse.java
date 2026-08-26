package se.portplanner.dto;

import java.math.BigDecimal;

public record PriceCalculationResponse(Long bookingId, String boatModel,
                                       BigDecimal lengthM, BigDecimal widthM,
                                       BigDecimal areaSqm, BigDecimal basePrice,
                                       BigDecimal widthSurcharge, BigDecimal lengthSurcharge,
                                       BigDecimal totalPrice) {}
