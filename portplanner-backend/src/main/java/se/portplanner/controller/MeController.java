package se.portplanner.controller;

import se.portplanner.module.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.*;
import se.portplanner.service.MeService;

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
}
