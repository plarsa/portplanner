package se.portplanner.dto;

import se.portplanner.model.RentalObject;

/** A rental object. {@code hasImage} tells a client whether the image endpoint will answer. */
public record RentalObjectResponse(
        Long id,
        String name,
        String description,
        boolean bookable,
        int maxDurationHours,
        boolean requiresApproval,
        int maxDaysAhead,
        boolean hasImage) {

    public static RentalObjectResponse from(RentalObject o) {
        return new RentalObjectResponse(o.getId(), o.getName(), o.getDescription(), o.isBookable(),
                o.getMaxDurationHours(), o.isRequiresApproval(), o.getMaxDaysAhead(), o.getImage() != null);
    }
}
