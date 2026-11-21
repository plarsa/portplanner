package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.portplanner.dto.ImportPreview;
import se.portplanner.dto.ImportResult;
import se.portplanner.dto.PortplannerExport;
import se.portplanner.module.RequiresModule;
import se.portplanner.service.ImportExportService;

@RestController
@RequiresModule("import-export")
@RequestMapping("/api")
@Tag(name = "Import / Export")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @GetMapping("/export")
    @Operation(summary = "Export the whole harbour as one JSON file, one section per area")
    public PortplannerExport export() {
        return importExportService.exportAll();
    }

    @PostMapping("/import/preview")
    @Operation(summary = "Preview an import without saving")
    public ImportPreview preview(@RequestBody PortplannerExport file) {
        return importExportService.previewAll(file);
    }

    @PostMapping("/import")
    @Operation(summary = "Import a file, one section per area")
    public ImportResult importFile(@RequestBody PortplannerExport file) {
        return importExportService.importAll(file);
    }
}
