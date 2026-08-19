package se.portplanner.dto;

import java.math.BigDecimal;

public record BoatImportDto(
        String name,
        String registrationNumber,
        BigDecimal lengthM,
        BigDecimal widthM,
        BigDecimal draftM,
        String ownerEmail
) {}
