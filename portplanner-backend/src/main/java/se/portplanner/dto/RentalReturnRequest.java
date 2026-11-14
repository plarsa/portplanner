package se.portplanner.dto;

/** A return. An empty or null description means the object came back without a fault. */
public record RentalReturnRequest(String faultDescription) {
}
