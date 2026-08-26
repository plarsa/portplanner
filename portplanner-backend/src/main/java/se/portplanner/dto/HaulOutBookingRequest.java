package se.portplanner.dto;

import jakarta.validation.constraints.NotNull;

public record HaulOutBookingRequest(
        @NotNull Long slotId,
        @NotNull Long boatId,
        @NotNull Long personId
) {}
