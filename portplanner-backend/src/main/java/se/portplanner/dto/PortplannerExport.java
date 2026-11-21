package se.portplanner.dto;

import java.util.List;

/**
 * The whole harbour in one file, one section per area. A section that is left
 * out means there is nothing to import for that area.
 */
public record PortplannerExport(
        List<PersonImportDto> persons,
        List<BoatImportDto> boats,
        List<DockImportDto> docks
) {}
