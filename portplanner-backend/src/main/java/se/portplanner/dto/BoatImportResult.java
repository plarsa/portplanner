package se.portplanner.dto;

import java.util.List;

public record BoatImportResult(int boatsCreated, int boatsSkipped, List<String> warnings) {}
