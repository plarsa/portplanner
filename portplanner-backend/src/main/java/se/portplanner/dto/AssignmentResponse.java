package se.portplanner.dto;

import se.portplanner.model.Assignment;
import se.portplanner.model.AssignmentStatus;

import java.time.LocalDate;

public record AssignmentResponse(
        Long id,
        Long boatId, String boatName,
        Long slipId, String slipNumber, String dockName,
        String ownerName,
        LocalDate assignedDate, LocalDate endDate,
        AssignmentStatus status) {

    public static AssignmentResponse from(Assignment a) {
        return new AssignmentResponse(
                a.getId(),
                a.getBoat().getId(), a.getBoat().getModel(),
                a.getSlip().getId(), a.getSlip().getSlipNumber(), a.getSlip().getDock().getName(),
                a.getBoat().getOwner().getFirstName() + " " + a.getBoat().getOwner().getLastName(),
                a.getAssignedDate(), a.getEndDate(), a.getStatus());
    }
}
