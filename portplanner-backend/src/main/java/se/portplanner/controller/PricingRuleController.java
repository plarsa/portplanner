package se.portplanner.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.PricingRuleRequest;
import se.portplanner.dto.PricingRuleResponse;
import se.portplanner.service.PricingRuleService;

import java.util.List;

@RestController
@RequestMapping("/api/pricing-rules")
@Tag(name = "Vinterupptagning – prisregler")
public class PricingRuleController {

    private final PricingRuleService service;

    public PricingRuleController(PricingRuleService service) {
        this.service = service;
    }

    @GetMapping
    public List<PricingRuleResponse> findBySeason(@RequestParam Long seasonId) {
        return service.findBySeason(seasonId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PricingRuleResponse create(@Valid @RequestBody PricingRuleRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public PricingRuleResponse update(@PathVariable Long id, @Valid @RequestBody PricingRuleRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
