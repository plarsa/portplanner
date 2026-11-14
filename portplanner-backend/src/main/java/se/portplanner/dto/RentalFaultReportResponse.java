package se.portplanner.dto;

import se.portplanner.model.RentalFaultReport;

import java.time.LocalDateTime;

public record RentalFaultReportResponse(
        Long id,
        Long bookingId,
        Long objectId,
        String objectName,
        String reportedBy,
        LocalDateTime reportedAt,
        String description,
        boolean resolved) {

    public static RentalFaultReportResponse from(RentalFaultReport r) {
        var booking = r.getBooking();
        var person = booking.getPerson();
        return new RentalFaultReportResponse(
                r.getId(), booking.getId(), booking.getObject().getId(), booking.getObject().getName(),
                person.getFirstName() + " " + person.getLastName(),
                r.getReportedAt(), r.getDescription(), r.getResolvedAt() != null);
    }
}
