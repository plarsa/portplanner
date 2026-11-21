package se.portplanner.dto;

/** What an import did, per area. */
public record ImportResult(
        PersonImportResult persons,
        BoatImportResult boats,
        DockImportResult docks
) {}
