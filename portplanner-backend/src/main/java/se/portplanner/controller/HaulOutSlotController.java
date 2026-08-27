package se.portplanner.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.HaulOutSlotRequest;
import se.portplanner.dto.HaulOutSlotResponse;
import se.portplanner.service.HaulOutSlotService;

import java.util.List;

@RestController
@RequestMapping("/api/haul-out-slots")
@Tag(name = "Vinterupptagning – tider")
public class HaulOutSlotController {

    private final HaulOutSlotService service;

    public HaulOutSlotController(HaulOutSlotService service) {
        this.service = service;
    }

    @GetMapping
    public List<HaulOutSlotResponse> findBySeason(@RequestParam Long seasonId) {
        return service.findBySeason(seasonId);
    }

    @GetMapping("/{id}")
    public HaulOutSlotResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HaulOutSlotResponse create(@Valid @RequestBody HaulOutSlotRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public HaulOutSlotResponse update(@PathVariable Long id, @Valid @RequestBody HaulOutSlotRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
