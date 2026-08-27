package se.portplanner.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.WinterSeasonRequest;
import se.portplanner.dto.WinterSeasonResponse;
import se.portplanner.service.WinterSeasonService;

import java.util.List;

@RestController
@RequestMapping("/api/winter-seasons")
@Tag(name = "Vinterupptagning – säsonger")
public class WinterSeasonController {

    private final WinterSeasonService service;

    public WinterSeasonController(WinterSeasonService service) {
        this.service = service;
    }

    @GetMapping
    public List<WinterSeasonResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public WinterSeasonResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WinterSeasonResponse create(@Valid @RequestBody WinterSeasonRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public WinterSeasonResponse update(@PathVariable Long id, @Valid @RequestBody WinterSeasonRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
