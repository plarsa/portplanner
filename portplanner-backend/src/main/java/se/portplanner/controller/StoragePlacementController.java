package se.portplanner.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.StoragePlacementRequest;
import se.portplanner.dto.StoragePlacementResponse;
import se.portplanner.service.StoragePlacementService;

import java.util.List;

@RestController
@RequestMapping("/api/storage-placements")
@Tag(name = "Vinterupptagning – placeringar")
public class StoragePlacementController {

    private final StoragePlacementService service;

    public StoragePlacementController(StoragePlacementService service) {
        this.service = service;
    }

    @GetMapping
    public List<StoragePlacementResponse> findByYard(@RequestParam Long yardId) {
        return service.findByYard(yardId);
    }

    @GetMapping("/{id}")
    public StoragePlacementResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public StoragePlacementResponse update(@PathVariable Long id, @RequestBody StoragePlacementRequest req) {
        return service.update(id, req);
    }

    @PutMapping("/{id}/launch")
    public StoragePlacementResponse launch(@PathVariable Long id) {
        return service.launch(id);
    }
}
