package se.portplanner.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** A member's booking request. The interval is half-open: {@code [startAt, endAt)}. */
public record RentalBookingRequest(
        @NotNull Long objectId,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt) {
}
