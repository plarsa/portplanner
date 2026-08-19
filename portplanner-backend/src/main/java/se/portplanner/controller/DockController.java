package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.DockRequest;
import se.portplanner.dto.DockResponse;
import se.portplanner.dto.SlipResponse;
import se.portplanner.service.DockService;

import java.util.List;

@RestController
@RequestMapping("/api/docks")
@Tag(name = "Bryggor")
public class DockController {

    private final DockService dockService;

    public DockController(DockService dockService) {
        this.dockService = dockService;
    }

    @GetMapping
    @Operation(summary = "Hämta alla bryggor")
    public List<DockResponse> findAll() {
        return dockService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Hämta brygga per id")
    public DockResponse findById(@PathVariable Long id) {
        return dockService.findById(id);
    }

    @GetMapping("/{id}/slips")
    @Operation(summary = "Hämta alla platser för en brygga")
    public List<SlipResponse> findSlips(@PathVariable Long id) {
        return dockService.findSlips(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Skapa brygga")
    public DockResponse create(@Valid @RequestBody DockRequest req) {
        return dockService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Uppdatera brygga")
    public DockResponse update(@PathVariable Long id, @Valid @RequestBody DockRequest req) {
        return dockService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ta bort brygga")
    public void delete(@PathVariable Long id) {
        dockService.delete(id);
    }
}
