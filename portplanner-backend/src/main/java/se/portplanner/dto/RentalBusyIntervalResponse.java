package se.portplanner.dto;

import java.time.LocalDateTime;

/**
 * A taken slot in an object's calendar. REN-4: the calendar shows that a time is busy and never
 * who booked it, so this record carries no person and no status, on purpose.
 */
public record RentalBusyIntervalResponse(LocalDateTime startAt, LocalDateTime endAt) {
}
