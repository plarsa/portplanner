package se.portplaner.dto;

import java.util.List;

public record PersonImportPreview(
        int personsNew,
        int personsExisting,
        List<String> details
) {}
