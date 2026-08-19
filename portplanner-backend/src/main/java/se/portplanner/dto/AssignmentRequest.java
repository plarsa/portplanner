package se.portplanner.dto;

import jakarta.validation.constraints.NotNull;

public record AssignmentRequest(@NotNull Long boatId, @NotNull Long slipId) {}
