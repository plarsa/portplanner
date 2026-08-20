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
@Tag(name = "Båtplatser")
public class SlipController {

    private final SlipService slipService;
    private final AssignmentService assignmentService;

    public SlipController(SlipService slipService, AssignmentService assignmentService) {
        this.slipService = slipService;
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @Operation(summary = "Hämta alla båtplatser")
    public List<SlipResponse> findAll() {
        return slipService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Hämta båtplats per id")
    public SlipResponse findById(@PathVariable Long id) {
        return slipService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Skapa båtplats")
    public SlipResponse create(@Valid @RequestBody SlipRequest req) {
        return slipService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Uppdatera båtplats")
    public SlipResponse update(@PathVariable Long id, @Valid @RequestBody SlipRequest req) {
        return slipService.update(id, req);
    }

    @GetMapping("/{id}/active-assignment")
    @Operation(summary = "Hämta aktiv tilldelning för en plats")
    public ResponseEntity<AssignmentResponse> activeAssignment(@PathVariable Long id) {
        return assignmentService.findActiveBySlip(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ta bort båtplats")
    public void delete(@PathVariable Long id) {
        slipService.delete(id);
    }
}
