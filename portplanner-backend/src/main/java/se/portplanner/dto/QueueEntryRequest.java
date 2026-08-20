package se.portplanner.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record QueueEntryRequest(@NotNull Long personId, Long boatId, String notes, LocalDate requestedDate) {}
