package se.portplanner.controller;

import se.portplanner.module.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.*;
import se.portplanner.service.MeService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiresModule("member-portal")
@RequestMapping("/api/me")
@Tag(name = "Member portal")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    @Operation(summary = "Get the signed-in member's personal details")
    public MeResponse getMe() {
        return meService.getMe();
    }

    @PutMapping
    @Operation(summary = "Update contact details (phone, address)")
    public MeResponse updateProfile(@RequestBody PersonRequest req) {
        return meService.updateProfile(req);
    }

    @GetMapping("/boats")
    @Operation(summary = "List my boats")
    public List<BoatResponse> getMyBoats() {
        return meService.getMyBoats();
    }

    @GetMapping("/assignments")
    @Operation(summary = "Get my active slip")
    public List<AssignmentResponse> getMyAssignments() {
        return meService.getMyAssignments();
    }

    @GetMapping("/queue")
    @Operation(summary = "Get my queue status, including any offer")
    public List<QueueEntryResponse> getMyQueue() {
        return meService.getMyQueue();
    }

    @RequiresModule("storage")
    @GetMapping("/haul-out-slots/available")
    @Operation(summary = "Available haul-out slots")
    public List<HaulOutSlotResponse> getAvailableHaulOutSlots() {
        return meService.getAvailableHaulOutSlots();
    }

    @RequiresModule("storage")
    @GetMapping("/haul-out-bookings")
    @Operation(summary = "My haul-out bookings")
    public List<HaulOutBookingResponse> getMyHaulOutBookings() {
        return meService.getMyHaulOutBookings();
    }

    @RequiresModule("storage")
    @PostMapping("/haul-out-bookings")
    @Operation(summary = "Book a boat for haul-out")
    public HaulOutBookingResponse createMyHaulOutBooking(@RequestParam Long slotId,
                                                          @RequestParam Long boatId) {
        return meService.createMyHaulOutBooking(slotId, boatId);
    }

    @RequiresModule("rentals")
    @GetMapping("/rentals/objects")
    @Operation(summary = "Rental objects open for booking")
    public List<RentalObjectResponse> getRentalObjects() {
        return meService.getRentalObjects();
    }

    @RequiresModule("rentals")
    @GetMapping("/rentals/objects/{id}/image")
    @Operation(summary = "A rental object's image")
    public ResponseEntity<InputStreamResource> getRentalObjectImage(@PathVariable Long id) {
        var content = meService.getRentalObjectImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.size())
                .body(new InputStreamResource(content.data()));
    }

    @RequiresModule("rentals")
    @GetMapping("/rentals/objects/{id}/availability")
    @Operation(summary = "The times already taken on a rental object")
    public List<RentalBusyIntervalResponse> getRentalAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return meService.getRentalAvailability(id, from, to);
    }

    @RequiresModule("rentals")
    @GetMapping("/rentals/bookings")
    @Operation(summary = "My rental bookings")
    public List<RentalBookingResponse> getMyRentalBookings() {
        return meService.getMyRentalBookings();
    }

    @RequiresModule("rentals")
    @PostMapping("/rentals/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a rental object")
    public RentalBookingResponse createMyRentalBooking(@Valid @RequestBody RentalBookingRequest req) {
        return meService.createMyRentalBooking(req);
    }

    @RequiresModule("rentals")
    @PostMapping("/rentals/bookings/{id}/return")
    @Operation(summary = "Return a rental object and optionally report a fault")
    public RentalBookingResponse returnMyRentalBooking(@PathVariable Long id,
                                                       @RequestBody(required = false) RentalReturnRequest req) {
        return meService.returnMyRentalBooking(id, req);
    }

    @RequiresModule("rentals")
    @DeleteMapping("/rentals/bookings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel one of my rental bookings")
    public void cancelMyRentalBooking(@PathVariable Long id) {
        meService.cancelMyRentalBooking(id);
    }
}
