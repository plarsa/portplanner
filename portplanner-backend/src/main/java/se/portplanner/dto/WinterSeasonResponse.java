package se.portplanner.dto;

import se.portplanner.model.WinterSeason;

import java.time.LocalDate;

public record WinterSeasonResponse(Long id, Integer year, String name,
                                   LocalDate startDate, LocalDate endDate, String status) {
    public static WinterSeasonResponse from(WinterSeason s) {
        return new WinterSeasonResponse(s.getId(), s.getYear(), s.getName(),
                s.getStartDate(), s.getEndDate(), s.getStatus().name());
    }
}
