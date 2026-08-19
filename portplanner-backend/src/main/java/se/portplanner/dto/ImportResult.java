package se.portplanner.dto;

import java.util.List;

public record ImportResult(
        int docksCreated,
        int slipsCreated,
        int slipsSkipped,
        List<String> warnings
) {}
