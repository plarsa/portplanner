package se.portplanner.dto;

import se.portplanner.model.HaulOutSlot;

import java.time.LocalDate;
import java.time.LocalTime;

public record HaulOutSlotResponse(Long id, Long seasonId, String seasonName,
                                  LocalDate slotDate, LocalTime startTime, LocalTime endTime,
                                  int capacity, int bookedCount) {
    public static HaulOutSlotResponse from(HaulOutSlot s, int bookedCount) {
        return new HaulOutSlotResponse(s.getId(), s.getSeason().getId(), s.getSeason().getName(),
                s.getSlotDate(), s.getStartTime(), s.getEndTime(),
                s.getCapacity(), bookedCount);
    }
}
