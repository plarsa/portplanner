package se.portplaner.dto;

import se.portplaner.model.Boat;

import java.math.BigDecimal;

public record BoatResponse(Long id, String name, String registrationNumber,
                           BigDecimal lengthM, BigDecimal widthM, BigDecimal draftM,
                           Long ownerId, String ownerName) {
    public static BoatResponse from(Boat b) {
        return new BoatResponse(b.getId(), b.getName(), b.getRegistrationNumber(),
                b.getLengthM(), b.getWidthM(), b.getDraftM(),
                b.getOwner().getId(),
                b.getOwner().getFirstName() + " " + b.getOwner().getLastName());
    }
}
