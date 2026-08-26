package se.portplanner.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record HaulOutSlotRequest(
        @NotNull Long seasonId,
        @NotNull LocalDate slotDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Positive Integer capacity
) {}
