package se.portplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StoragePackingGroupRequest(
        @NotNull Long yardId,
        @NotBlank String name,
        String retrievalNote
) {}
