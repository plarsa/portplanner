package se.portplanner.dto;

import java.math.BigDecimal;

public record BoatImportDto(
        String model,
        BigDecimal lengthM,
        BigDecimal widthM,
        BigDecimal draftM,
        String ownerEmail
) {}
