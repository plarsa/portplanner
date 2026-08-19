package se.portplaner.dto;

import java.util.List;

public record PersonImportResult(
        int personsCreated,
        int personsSkipped,
        List<String> warnings
) {}
