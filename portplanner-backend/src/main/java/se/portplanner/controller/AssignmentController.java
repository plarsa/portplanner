package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.AssignmentRequest;
import se.portplanner.dto.AssignmentResponse;
import se.portplanner.service.AssignmentService;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@Tag(name = "Tilldelningar")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @Operation(summary = "Hämta aktiva tilldelningar")
    public List<AssignmentResponse> findActive() {
        return assignmentService.findActive();
    }

    @GetMapping("/boat/{boatId}")
    @Operation(summary = "Hämta tilldelningshistorik för en båt")
    public List<AssignmentResponse> findByBoat(@PathVariable Long boatId) {
        return assignmentService.findByBoat(boatId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tilldela båt till plats (validerar mått)")
    public AssignmentResponse create(@Valid @RequestBody AssignmentRequest req) {
        return assignmentService.create(req);
    }

    @PutMapping("/{id}/end")
    @Operation(summary = "Avsluta tilldelning och frigör platsen")
    public AssignmentResponse end(@PathVariable Long id) {
        return assignmentService.end(id);
    }
}
