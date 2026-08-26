package se.portplanner.dto;

import java.math.BigDecimal;
import java.util.List;

public record StorageYardCalibrationRequest(
        Double originPixelX,
        Double originPixelY,
        Double pixelsPerMeter,
        List<BoundaryPointDto> boundary
) {}
