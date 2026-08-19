package se.portplaner.dto;

import jakarta.validation.constraints.NotNull;

public record QueueEntryRequest(@NotNull Long personId, Long boatId, String notes) {}
