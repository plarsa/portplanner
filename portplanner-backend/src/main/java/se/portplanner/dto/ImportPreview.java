package se.portplanner.dto;

/** What an import would do, per area, without saving anything. */
public record ImportPreview(
        PersonImportPreview persons,
        BoatImportPreview boats,
        DockImportPreview docks
) {}
