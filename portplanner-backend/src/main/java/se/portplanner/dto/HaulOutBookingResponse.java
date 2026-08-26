package se.portplanner.dto;

import se.portplanner.model.HaulOutBooking;

import java.time.LocalDateTime;

public record HaulOutBookingResponse(Long id, Long slotId, String slotDate, String slotTime,
                                     Long boatId, String boatModel,
                                     Long personId, String personName,
                                     LocalDateTime requestedAt, String status) {
    public static HaulOutBookingResponse from(HaulOutBooking b) {
        var slot = b.getSlot();
        return new HaulOutBookingResponse(
                b.getId(), slot.getId(),
                slot.getSlotDate().toString(),
                slot.getStartTime() + "–" + slot.getEndTime(),
                b.getBoat().getId(), b.getBoat().getModel(),
                b.getPerson().getId(),
                b.getPerson().getFirstName() + " " + b.getPerson().getLastName(),
                b.getRequestedAt(), b.getStatus().name());
    }
}
