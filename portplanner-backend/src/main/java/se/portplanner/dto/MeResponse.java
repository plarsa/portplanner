package se.portplanner.dto;

import se.portplanner.model.Person;

public record MeResponse(
        Long personId,
        String firstName, String lastName,
        String email, String phone,
        String address, String postalCode,
        String propertyDesignation, String notes) {

    public static MeResponse from(Person p) {
        return new MeResponse(p.getId(), p.getFirstName(), p.getLastName(),
                p.getEmail(), p.getPhone(), p.getAddress(), p.getPostalCode(),
                p.getPropertyDesignation(), p.getNotes());
    }
}
