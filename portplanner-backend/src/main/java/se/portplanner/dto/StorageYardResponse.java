package se.portplanner.dto;

import se.portplanner.model.StorageYard;

import java.math.BigDecimal;
import java.util.List;

public record StorageYardResponse(Long id, Long seasonId, String seasonName, String name,
                                  String backgroundImageUrl,
                                  Double originPixelX, Double originPixelY, Double pixelsPerMeter,
                                  BigDecimal laneMarginM,
                                  List<BoundaryPointDto> boundary) {
    public static StorageYardResponse from(StorageYard y) {
        var pts = y.getBoundary().stream()
                .map(p -> new BoundaryPointDto(p.getXMeters(), p.getYMeters()))
                .toList();
        return new StorageYardResponse(y.getId(), y.getSeason().getId(), y.getSeason().getName(),
                y.getName(), y.getBackgroundImageUrl(),
                y.getOriginPixelX(), y.getOriginPixelY(), y.getPixelsPerMeter(),
                y.getLaneMarginM(), pts);
    }
}
