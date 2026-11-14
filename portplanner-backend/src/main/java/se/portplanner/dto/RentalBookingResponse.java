package se.portplanner.dto;

import se.portplanner.model.RentalBooking;

import java.time.LocalDateTime;

/**
 * A booking. The person is named so an administrator can tell whose it is (REN-10); a member only
 * ever receives their own rows here. The object's calendar uses RentalBusyIntervalResponse, which
 * carries no person, so REN-4 is unaffected.
 */
public record RentalBookingResponse(
        Long id,
        Long objectId,
        String objectName,
        Long personId,
        String personName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        LocalDateTime returnedAt) {

    public static RentalBookingResponse from(RentalBooking b) {
        var person = b.getPerson();
        return new RentalBookingResponse(b.getId(), b.getObject().getId(), b.getObject().getName(),
                person.getId(), person.getFirstName() + " " + person.getLastName(),
                b.getStartAt(), b.getEndAt(), b.getStatus().name(), b.getReturnedAt());
    }
}
