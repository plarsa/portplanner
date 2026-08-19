package se.portplaner.dto;

import se.portplaner.model.Tariff;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffResponse(
        Long id,
        String name,
        String category,
        BigDecimal annualFeeKr,
        LocalDate validFrom,
        LocalDate validTo,
        String description
) {
    public static TariffResponse from(Tariff t) {
        return new TariffResponse(
                t.getId(), t.getName(), t.getCategory(),
                t.getAnnualFeeKr(), t.getValidFrom(), t.getValidTo(),
                t.getDescription());
    }
}
