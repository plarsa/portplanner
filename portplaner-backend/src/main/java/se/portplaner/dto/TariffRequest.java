package se.portplaner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @Positive BigDecimal annualFeeKr,
        @NotNull LocalDate validFrom,
        LocalDate validTo,
        String description
) {}
