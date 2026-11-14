package se.portplanner.model;

import java.util.Set;

/** Where a rental booking stands. */
public enum RentalBookingStatus {
    /** Awaiting an administrator's decision; it already holds the time (REN-6). */
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    RETURNED;

    /** The states that occupy the calendar, so a new booking may not overlap them. */
    public static Set<RentalBookingStatus> holding() {
        return Set.of(PENDING, CONFIRMED, RETURNED);
    }
}
