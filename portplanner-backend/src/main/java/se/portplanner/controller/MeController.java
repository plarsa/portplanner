package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.*;
import se.portplanner.service.MeService;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Medlemsportal")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping
    @Operation(summary = "Hämta inloggad medlems personuppgifter")
    public MeResponse getMe() {
        return meService.getMe();
    }

    @PutMapping
    @Operation(summary = "Uppdatera kontaktuppgifter (telefon, adress)")
    public MeResponse updateProfile(@RequestBody PersonRequest req) {
        return meService.updateProfile(req);
    }

    @GetMapping("/boats")
    @Operation(summary = "Hämta mina båtar")
    public List<BoatResponse> getMyBoats() {
        return meService.getMyBoats();
    }

    @GetMapping("/assignments")
    @Operation(summary = "Hämta min aktiva bryggplats")
    public List<AssignmentResponse> getMyAssignments() {
        return meService.getMyAssignments();
    }

    @GetMapping("/queue")
    @Operation(summary = "Hämta min köstatus (inkl. ev. erbjudande)")
    public List<QueueEntryResponse> getMyQueue() {
        return meService.getMyQueue();
    }

    @GetMapping("/haul-out-slots/available")
    @Operation(summary = "Lediga upptagningstider")
    public List<HaulOutSlotResponse> getAvailableHaulOutSlots() {
        return meService.getAvailableHaulOutSlots();
    }

    @GetMapping("/haul-out-bookings")
    @Operation(summary = "Mina vinterupptagningsanmälningar")
    public List<HaulOutBookingResponse> getMyHaulOutBookings() {
        return meService.getMyHaulOutBookings();
    }

    @PostMapping("/haul-out-bookings")
    @Operation(summary = "Anmäl båt till vinterupptagning")
    public HaulOutBookingResponse createMyHaulOutBooking(@RequestParam Long slotId,
                                                          @RequestParam Long boatId) {
        return meService.createMyHaulOutBooking(slotId, boatId);
    }
}
