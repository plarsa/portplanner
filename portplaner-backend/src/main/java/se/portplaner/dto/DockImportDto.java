package se.portplaner.dto;

import java.util.List;

public record DockImportDto(
        String name,
        String description,
        List<SlipImportDto> slips
) {}
