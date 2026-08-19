package se.portplaner.dto;

import se.portplaner.model.QueueEntry;
import se.portplaner.model.QueueEntryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QueueEntryResponse(
        Long id,
        Long personId, String personName,
        Long boatId, String boatName,
        BigDecimal boatLengthM, BigDecimal boatWidthM, BigDecimal boatDraftM,
        LocalDateTime requestedDate, String notes,
        QueueEntryStatus status) {

    public static QueueEntryResponse from(QueueEntry e) {
        var boat = e.getBoat();
        return new QueueEntryResponse(
                e.getId(),
                e.getPerson().getId(),
                e.getPerson().getFirstName() + " " + e.getPerson().getLastName(),
                boat != null ? boat.getId() : null,
                boat != null ? boat.getName() : null,
                boat != null ? boat.getLengthM() : null,
                boat != null ? boat.getWidthM() : null,
                boat != null ? boat.getDraftM() : null,
                e.getRequestedDate(), e.getNotes(), e.getStatus());
    }
}
