package se.portplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import se.portplanner.model.SlipStatus;

import java.math.BigDecimal;

public record SlipRequest(
        @NotBlank String slipNumber,
        @NotNull @Positive BigDecimal maxLengthM,
        @NotNull @Positive BigDecimal maxWidthM,
        @Positive BigDecimal maxDraftM,
        @NotNull Long dockId,
        SlipStatus status,
        String category,
        String notes
) {}
