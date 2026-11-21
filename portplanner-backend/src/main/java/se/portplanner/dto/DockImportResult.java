package se.portplanner.dto;

import java.util.List;

public record DockImportResult(
        int docksCreated,
        int slipsCreated,
        int slipsSkipped,
        List<String> warnings
) {}
