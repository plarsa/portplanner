package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.RentalObjectRequest;
import se.portplanner.dto.RentalObjectResponse;
import se.portplanner.module.RequiresModule;
import se.portplanner.service.RentalObjectService;

import java.util.List;

@RestController
@RequiresModule("rentals")
@RequestMapping("/api/rentals/objects")
@Tag(name = "Rentals – objects")
public class RentalObjectController {

    private final RentalObjectService service;

    public RentalObjectController(RentalObjectService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all rental objects")
    public List<RentalObjectResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a rental object by id")
    public RentalObjectResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a rental object")
    public RentalObjectResponse create(@Valid @RequestBody RentalObjectRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a rental object")
    public RentalObjectResponse update(@PathVariable Long id, @Valid @RequestBody RentalObjectRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a rental object")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
