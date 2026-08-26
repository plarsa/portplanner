package se.portplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StorageYardRequest(
        @NotNull Long seasonId,
        @NotBlank String name,
        String backgroundImageUrl,
        BigDecimal laneMarginM
) {}
