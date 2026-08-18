package se.portplaner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplaner.dto.AssignmentResponse;
import se.portplaner.dto.QueueEntryRequest;
import se.portplaner.dto.QueueEntryResponse;
import se.portplaner.dto.SlipResponse;
import se.portplaner.service.QueueService;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
@Tag(name = "Kö")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    @Operation(summary = "Hämta kölistan (FIFO, status WAITING)")
    public List<QueueEntryResponse> findWaiting() {
        return queueService.findWaiting();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Lägg till person+båt i kö")
    public QueueEntryResponse add(@Valid @RequestBody QueueEntryRequest req) {
        return queueService.add(req);
    }

    @GetMapping("/{id}/suggestions")
    @Operation(summary = "Föreslå lediga platser som passar båtens mått")
    public List<SlipResponse> suggestions(@PathVariable Long id) {
        return queueService.suggestions(id);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Tilldela plats från kö (tar bort ur kön)")
    public AssignmentResponse assign(@PathVariable Long id, @RequestParam Long slipId) {
        return queueService.assignFromQueue(id, slipId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Avboka kö-post")
    public void cancel(@PathVariable Long id) {
        queueService.cancel(id);
    }
}
