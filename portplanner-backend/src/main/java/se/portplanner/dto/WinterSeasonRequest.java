package se.portplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WinterSeasonRequest(
        @NotNull Integer year,
        @NotBlank String name,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {}
