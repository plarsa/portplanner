package se.portplaner.dto;

import java.util.List;

public record ImportPreview(
        int docksNew,
        int docksExisting,
        int slipsNew,
        int slipsSkipped,
        List<String> details
) {}
