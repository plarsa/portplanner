package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.TariffRequest;
import se.portplanner.dto.TariffResponse;
import se.portplanner.service.TariffService;

import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
@Tag(name = "Tariffs")
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @GetMapping
    @Operation(summary = "List all tariffs")
    public List<TariffResponse> findAll() {
        return tariffService.findAll();
    }

    @GetMapping("/active")
    @Operation(summary = "List the tariffs in force today")
    public List<TariffResponse> findActive() {
        return tariffService.findActiveToday();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a tariff")
    public TariffResponse create(@Valid @RequestBody TariffRequest req) {
        return tariffService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a tariff")
    public TariffResponse update(@PathVariable Long id, @Valid @RequestBody TariffRequest req) {
        return tariffService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a tariff")
    public void delete(@PathVariable Long id) {
        tariffService.delete(id);
    }
}
