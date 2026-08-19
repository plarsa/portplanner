package se.portplaner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplaner.dto.TariffRequest;
import se.portplaner.dto.TariffResponse;
import se.portplaner.service.TariffService;

import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
@Tag(name = "Taxor")
public class TariffController {

    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @GetMapping
    @Operation(summary = "Hämta alla taxor")
    public List<TariffResponse> findAll() {
        return tariffService.findAll();
    }

    @GetMapping("/active")
    @Operation(summary = "Hämta gällande taxor (aktiva idag)")
    public List<TariffResponse> findActive() {
        return tariffService.findActiveToday();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Skapa taxa")
    public TariffResponse create(@Valid @RequestBody TariffRequest req) {
        return tariffService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Uppdatera taxa")
    public TariffResponse update(@PathVariable Long id, @Valid @RequestBody TariffRequest req) {
        return tariffService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ta bort taxa")
    public void delete(@PathVariable Long id) {
        tariffService.delete(id);
    }
}
