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
        return new QueueEntryResponse(
                e.getId(),
                e.getPerson().getId(),
                e.getPerson().getFirstName() + " " + e.getPerson().getLastName(),
                e.getBoat().getId(), e.getBoat().getName(),
                e.getBoat().getLengthM(), e.getBoat().getWidthM(), e.getBoat().getDraftM(),
                e.getRequestedDate(), e.getNotes(), e.getStatus());
    }
}
