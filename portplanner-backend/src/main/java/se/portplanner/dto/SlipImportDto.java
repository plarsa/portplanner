package se.portplanner.dto;

import java.math.BigDecimal;

public record SlipImportDto(
        String slipNumber,
        BigDecimal maxWidthM,
        BigDecimal maxLengthM,
        BigDecimal maxDraftM,
        String mooringType,
        String side
) {}
