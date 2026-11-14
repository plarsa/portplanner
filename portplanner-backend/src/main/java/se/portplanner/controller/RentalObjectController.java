package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping("/{id}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Upload or replace the object's image")
    public void uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        service.uploadImage(id, file);
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Fetch the object's image")
    public ResponseEntity<InputStreamResource> image(@PathVariable Long id) {
        var content = service.readImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.size())
                .body(new InputStreamResource(content.data()));
    }

    @DeleteMapping("/{id}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove the object's image")
    public void deleteImage(@PathVariable Long id) {
        service.deleteImage(id);
    }
}
