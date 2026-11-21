package se.portplanner.dto;

import java.util.List;

public record DockImportPreview(
        int docksNew,
        int docksExisting,
        int slipsNew,
        int slipsSkipped,
        List<String> details
) {}
