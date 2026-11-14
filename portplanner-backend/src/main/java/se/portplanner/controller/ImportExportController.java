package se.portplanner.controller;

import se.portplanner.module.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.*;
import se.portplanner.service.ImportExportService;

import java.util.List;

@RestController
@RequiresModule("import-export")
@RequestMapping("/api")
@Tag(name = "Import / Export")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @GetMapping("/export/docks")
    @Operation(summary = "Export all docks and slips as JSON")
    public List<DockImportDto> exportDocks() {
        return importExportService.exportDocks();
    }

    @PostMapping("/import/docks/preview")
    @Operation(summary = "Preview an import without saving")
    public ImportPreview preview(@RequestBody List<DockImportDto> docks) {
        return importExportService.preview(docks);
    }

    @PostMapping("/import/docks")
    @Operation(summary = "Import docks and slips from JSON")
    public ImportResult importDocks(@RequestBody List<DockImportDto> docks) {
        return importExportService.importDocks(docks);
    }

    @GetMapping("/export/persons")
    @Operation(summary = "Export all persons as JSON")
    public List<PersonImportDto> exportPersons() {
        return importExportService.exportPersons();
    }

    @PostMapping("/import/persons/preview")
    @Operation(summary = "Preview a person import without saving")
    public PersonImportPreview previewPersons(@RequestBody List<PersonImportDto> persons) {
        return importExportService.previewPersons(persons);
    }

    @PostMapping("/import/persons")
    @Operation(summary = "Import persons from JSON")
    public PersonImportResult importPersons(@RequestBody List<PersonImportDto> persons) {
        return importExportService.importPersons(persons);
    }

    @GetMapping("/export/boats")
    @Operation(summary = "Export all boats as JSON")
    public List<BoatImportDto> exportBoats() {
        return importExportService.exportBoats();
    }

    @PostMapping("/import/boats/preview")
    @Operation(summary = "Preview a boat import without saving")
    public BoatImportPreview previewBoats(@RequestBody List<BoatImportDto> boats) {
        return importExportService.previewBoats(boats);
    }

    @PostMapping("/import/boats")
    @Operation(summary = "Import boats from JSON")
    public BoatImportResult importBoats(@RequestBody List<BoatImportDto> boats) {
        return importExportService.importBoats(boats);
    }
}
