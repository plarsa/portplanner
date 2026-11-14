package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.BoatRequest;
import se.portplanner.dto.BoatResponse;
import se.portplanner.service.BoatService;

import java.util.List;

@RestController
@RequestMapping("/api/boats")
@Tag(name = "Boats")
public class BoatController {

    private final BoatService boatService;

    public BoatController(BoatService boatService) {
        this.boatService = boatService;
    }

    @GetMapping
    @Operation(summary = "List all boats (optionally filtered by owner)")
    public List<BoatResponse> findAll(@RequestParam(required = false) Long ownerId) {
        return ownerId != null ? boatService.findByOwner(ownerId) : boatService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a boat by id")
    public BoatResponse findById(@PathVariable Long id) {
        return boatService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a boat")
    public BoatResponse create(@Valid @RequestBody BoatRequest req) {
        return boatService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a boat")
    public BoatResponse update(@PathVariable Long id, @Valid @RequestBody BoatRequest req) {
        return boatService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a boat")
    public void delete(@PathVariable Long id) {
        boatService.delete(id);
    }
}
