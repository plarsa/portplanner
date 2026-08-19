package se.portplanner.dto;

import java.util.List;

public record PersonImportResult(
        int personsCreated,
        int personsSkipped,
        List<String> warnings
) {}
