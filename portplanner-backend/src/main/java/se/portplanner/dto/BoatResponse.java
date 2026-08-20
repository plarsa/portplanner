package se.portplanner.dto;

import se.portplanner.model.Boat;

import java.math.BigDecimal;

public record BoatResponse(Long id, String model,
                           BigDecimal lengthM, BigDecimal widthM, BigDecimal draftM,
                           Long ownerId, String ownerName) {
    public static BoatResponse from(Boat b) {
        return new BoatResponse(b.getId(), b.getModel(),
                b.getLengthM(), b.getWidthM(), b.getDraftM(),
                b.getOwner().getId(),
                b.getOwner().getFirstName() + " " + b.getOwner().getLastName());
    }
}
