package se.portplanner.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** REN-12: an administrator books on a member's behalf, so the person is given explicitly. */
public record RentalAdminBookingRequest(
        @NotNull Long objectId,
        @NotNull Long personId,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt) {
}
