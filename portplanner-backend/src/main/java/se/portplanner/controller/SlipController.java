package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.AssignmentResponse;
import se.portplanner.dto.SlipRequest;
import se.portplanner.dto.SlipResponse;
import se.portplanner.service.AssignmentService;
import se.portplanner.service.SlipService;

import java.util.List;

@RestController
@RequestMapping("/api/slips")
@Tag(name = "Slips")
public class SlipController {

    private final SlipService slipService;
    private final AssignmentService assignmentService;

    public SlipController(SlipService slipService, AssignmentService assignmentService) {
        this.slipService = slipService;
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @Operation(summary = "List all slips")
    public List<SlipResponse> findAll() {
        return slipService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a slip by id")
    public SlipResponse findById(@PathVariable Long id) {
        return slipService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a slip")
    public SlipResponse create(@Valid @RequestBody SlipRequest req) {
        return slipService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a slip")
    public SlipResponse update(@PathVariable Long id, @Valid @RequestBody SlipRequest req) {
        return slipService.update(id, req);
    }

    @GetMapping("/{id}/active-assignment")
    @Operation(summary = "Get the active assignment for a slip")
    public ResponseEntity<AssignmentResponse> activeAssignment(@PathVariable Long id) {
        return assignmentService.findActiveBySlip(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a slip")
    public void delete(@PathVariable Long id) {
        slipService.delete(id);
    }
}
