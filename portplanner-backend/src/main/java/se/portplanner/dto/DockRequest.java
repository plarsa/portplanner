package se.portplanner.dto;

import jakarta.validation.constraints.NotBlank;

public record DockRequest(@NotBlank String name, String description) {}
