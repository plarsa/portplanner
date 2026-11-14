package se.portplanner.dto;

import se.portplanner.model.RentalBooking;

import java.time.LocalDateTime;

public record RentalBookingResponse(
        Long id,
        Long objectId,
        String objectName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        LocalDateTime returnedAt) {

    public static RentalBookingResponse from(RentalBooking b) {
        return new RentalBookingResponse(b.getId(), b.getObject().getId(), b.getObject().getName(),
                b.getStartAt(), b.getEndAt(), b.getStatus().name(), b.getReturnedAt());
    }
}
