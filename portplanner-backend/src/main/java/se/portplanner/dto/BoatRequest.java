package se.portplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BoatRequest(
        @NotBlank String model,
        @NotNull @Positive BigDecimal lengthM,
        @NotNull @Positive BigDecimal widthM,
        @Positive BigDecimal draftM,
        @NotNull Long ownerId,
        String hullType
) {}
