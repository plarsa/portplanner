package se.portplanner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.portplanner.dto.AssignmentResponse;
import se.portplanner.dto.QueueEntryRequest;
import se.portplanner.dto.QueueEntryResponse;
import se.portplanner.dto.SlipResponse;
import se.portplanner.service.QueueService;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
@Tag(name = "Queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    @Operation(summary = "List the queue (FIFO, status WAITING)")
    public List<QueueEntryResponse> findWaiting() {
        return queueService.findWaiting();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a person and boat to the queue")
    public QueueEntryResponse add(@Valid @RequestBody QueueEntryRequest req) {
        return queueService.add(req);
    }

    @GetMapping("/{id}/suggestions")
    @Operation(summary = "Suggest available slips that fit the boat's dimensions")
    public List<SlipResponse> suggestions(@PathVariable Long id) {
        return queueService.suggestions(id);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign a slip from the queue (removes the entry)")
    public AssignmentResponse assign(@PathVariable Long id, @RequestParam Long slipId) {
        return queueService.assignFromQueue(id, slipId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a queue entry (note, boat, date)")
    public QueueEntryResponse update(@PathVariable Long id, @Valid @RequestBody QueueEntryRequest req) {
        return queueService.update(id, req);
    }

    @PostMapping("/{id}/offer")
    @Operation(summary = "Offer a specific slip to the queued person (admin)")
    public QueueEntryResponse offer(@PathVariable Long id, @RequestParam Long slipId) {
        return queueService.offer(id, slipId);
    }

    @PostMapping("/{id}/accept-offer")
    @Operation(summary = "Member accepts an offered slip")
    public AssignmentResponse acceptOffer(@PathVariable Long id) {
        return queueService.acceptOffer(id);
    }

    @PostMapping("/{id}/decline-offer")
    @Operation(summary = "Member declines an offered slip (keeps their queue position)")
    public QueueEntryResponse declineOffer(@PathVariable Long id) {
        return queueService.declineOffer(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a queue entry")
    public void cancel(@PathVariable Long id) {
        queueService.cancel(id);
    }
}
