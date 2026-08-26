package se.portplanner.dto;

import java.math.BigDecimal;

public record StoragePlacementRequest(
        BigDecimal xMeters,
        BigDecimal yMeters,
        BigDecimal rotationDeg,
        BigDecimal widthMeters,
        BigDecimal lengthMeters,
        Long packingGroupId,
        Integer orderInGroup,
        String status
) {}
