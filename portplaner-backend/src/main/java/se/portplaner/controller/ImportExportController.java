package se.portplaner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import se.portplaner.dto.*;
import se.portplaner.service.ImportExportService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Import / Export")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @GetMapping("/export/docks")
    @Operation(summary = "Exportera alla bryggor och platser som JSON")
    public List<DockImportDto> exportDocks() {
        return importExportService.exportDocks();
    }

    @PostMapping("/import/docks/preview")
    @Operation(summary = "Förhandsgranska import utan att spara")
    public ImportPreview preview(@RequestBody List<DockImportDto> docks) {
        return importExportService.preview(docks);
    }

    @PostMapping("/import/docks")
    @Operation(summary = "Importera bryggor och platser från JSON")
    public ImportResult importDocks(@RequestBody List<DockImportDto> docks) {
        return importExportService.importDocks(docks);
    }

    @GetMapping("/export/persons")
    @Operation(summary = "Exportera alla personer som JSON")
    public List<PersonImportDto> exportPersons() {
        return importExportService.exportPersons();
    }

    @PostMapping("/import/persons/preview")
    @Operation(summary = "Förhandsgranska personimport utan att spara")
    public PersonImportPreview previewPersons(@RequestBody List<PersonImportDto> persons) {
        return importExportService.previewPersons(persons);
    }

    @PostMapping("/import/persons")
    @Operation(summary = "Importera personer från JSON")
    public PersonImportResult importPersons(@RequestBody List<PersonImportDto> persons) {
        return importExportService.importPersons(persons);
    }
}
