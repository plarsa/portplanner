package se.portplanner.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.StoragePackingGroupRequest;
import se.portplanner.dto.StoragePackingGroupResponse;
import se.portplanner.service.StoragePackingGroupService;

import java.util.List;

@RestController
@RequestMapping("/api/storage-packing-groups")
@Tag(name = "Vinterupptagning – packningsgrupper")
public class StoragePackingGroupController {

    private final StoragePackingGroupService service;

    public StoragePackingGroupController(StoragePackingGroupService service) {
        this.service = service;
    }

    @GetMapping
    public List<StoragePackingGroupResponse> findByYard(@RequestParam Long yardId) {
        return service.findByYard(yardId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoragePackingGroupResponse create(@Valid @RequestBody StoragePackingGroupRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public StoragePackingGroupResponse update(@PathVariable Long id,
                                               @Valid @RequestBody StoragePackingGroupRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
