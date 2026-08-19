package se.portplanner.dto;

import java.util.List;

public record BoatImportPreview(int boatsNew, int boatsExisting, int ownersNotFound, List<String> details) {}
