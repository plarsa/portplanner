package se.portplanner.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.HaulOutBookingRequest;
import se.portplanner.dto.HaulOutBookingResponse;
import se.portplanner.dto.PriceCalculationResponse;
import se.portplanner.service.HaulOutBookingService;
import se.portplanner.service.PricingRuleService;

import java.util.List;

@RestController
@RequestMapping("/api/haul-out-bookings")
@Tag(name = "Vinterupptagning – anmälningar")
public class HaulOutBookingController {

    private final HaulOutBookingService bookingService;
    private final PricingRuleService pricingService;

    public HaulOutBookingController(HaulOutBookingService bookingService, PricingRuleService pricingService) {
        this.bookingService = bookingService;
        this.pricingService = pricingService;
    }

    @GetMapping
    public List<HaulOutBookingResponse> find(@RequestParam(required = false) Long slotId,
                                              @RequestParam(required = false) Long seasonId) {
        if (slotId != null) return bookingService.findBySlot(slotId);
        if (seasonId != null) return bookingService.findBySeason(seasonId);
        return List.of();
    }

    @GetMapping("/{id}")
    public HaulOutBookingResponse findById(@PathVariable Long id) {
        return bookingService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HaulOutBookingResponse create(@Valid @RequestBody HaulOutBookingRequest req) {
        return bookingService.create(req);
    }

    @PutMapping("/{id}/confirm")
    public HaulOutBookingResponse confirm(@PathVariable Long id) {
        return bookingService.confirm(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookingService.delete(id);
    }

    @GetMapping("/{id}/price")
    public PriceCalculationResponse price(@PathVariable Long id) {
        return pricingService.calculateForBooking(id);
    }
}
