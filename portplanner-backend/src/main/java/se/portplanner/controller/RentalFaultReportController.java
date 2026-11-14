package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.RentalFaultReportResponse;
import se.portplanner.module.RequiresModule;
import se.portplanner.service.RentalBookingService;

import java.util.List;

/** REN-9: the administrator's view of reported faults. */
@RestController
@RequiresModule("rentals")
@RequestMapping("/api/rentals/fault-reports")
@Tag(name = "Rentals – fault reports")
public class RentalFaultReportController {

    private final RentalBookingService service;

    public RentalFaultReportController(RentalBookingService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List fault reports, open ones by default")
    public List<RentalFaultReportResponse> find(
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) Long objectId) {
        if (objectId != null) {
            return service.faultsForObject(objectId);
        }
        return Boolean.TRUE.equals(resolved) ? service.resolvedFaults() : service.openFaults();
    }

    @PostMapping("/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark a fault report as handled")
    public void resolve(@PathVariable Long id) {
        service.resolveFault(id);
    }
}
