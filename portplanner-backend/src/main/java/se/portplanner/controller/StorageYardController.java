package se.portplanner.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.*;
import se.portplanner.service.StoragePlacementService;
import se.portplanner.service.StorageYardService;

import java.util.List;

@RestController
@RequestMapping("/api/storage-yards")
@Tag(name = "Vinterupptagning – gårdsplaner")
public class StorageYardController {

    private final StorageYardService yardService;
    private final StoragePlacementService placementService;

    public StorageYardController(StorageYardService yardService, StoragePlacementService placementService) {
        this.yardService = yardService;
        this.placementService = placementService;
    }

    @GetMapping
    public List<StorageYardResponse> findBySeason(@RequestParam Long seasonId) {
        return yardService.findBySeason(seasonId);
    }

    @GetMapping("/{id}")
    public StorageYardResponse findById(@PathVariable Long id) {
        return yardService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StorageYardResponse create(@Valid @RequestBody StorageYardRequest req) {
        return yardService.create(req);
    }

    @PutMapping("/{id}")
    public StorageYardResponse update(@PathVariable Long id, @Valid @RequestBody StorageYardRequest req) {
        return yardService.update(id, req);
    }

    @PutMapping("/{id}/calibration")
    public StorageYardResponse calibrate(@PathVariable Long id,
                                          @RequestBody StorageYardCalibrationRequest req) {
        return yardService.calibrate(id, req);
    }

    @PostMapping("/{id}/suggest-placement")
    public List<StoragePlacementResponse> suggestPlacements(@PathVariable Long id) {
        return yardService.suggestPlacements(id);
    }

    @PostMapping("/{id}/validate")
    public List<ValidationIssue> validate(@PathVariable Long id) {
        return yardService.validate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        yardService.delete(id);
    }
}
