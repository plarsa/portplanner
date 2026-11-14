package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.RentalAdminBookingRequest;
import se.portplanner.dto.RentalBookingResponse;
import se.portplanner.dto.RentalReturnRequest;
import se.portplanner.module.RequiresModule;
import se.portplanner.service.RentalBookingService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiresModule("rentals")
@RequestMapping("/api/rentals/bookings")
@Tag(name = "Rentals – bookings")
public class RentalBookingController {

    private final RentalBookingService service;

    public RentalBookingController(RentalBookingService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List the bookings on an object in an interval")
    public List<RentalBookingResponse> findByObject(
            @RequestParam Long objectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.findByObject(objectId, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book an object on behalf of a member")
    public RentalBookingResponse bookForPerson(@Valid @RequestBody RentalAdminBookingRequest req) {
        return service.bookForPerson(req);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending booking")
    public RentalBookingResponse approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending booking")
    public RentalBookingResponse reject(@PathVariable Long id) {
        return service.reject(id);
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Mark a booking returned on behalf of a member")
    public RentalBookingResponse markReturned(@PathVariable Long id,
                                              @RequestBody(required = false) RentalReturnRequest req) {
        return service.markReturned(id, null, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a booking on behalf of a member")
    public void cancel(@PathVariable Long id) {
        service.cancel(id, null);
    }
}
