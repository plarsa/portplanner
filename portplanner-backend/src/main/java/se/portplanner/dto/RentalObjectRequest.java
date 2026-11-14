package se.portplanner.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Creates or replaces a rental object. The image is uploaded separately. */
public record RentalObjectRequest(
        @NotBlank String name,
        String description,
        boolean bookable,
        @Min(1) int maxDurationHours,
        boolean requiresApproval,
        @Min(1) int maxDaysAhead) {
}
