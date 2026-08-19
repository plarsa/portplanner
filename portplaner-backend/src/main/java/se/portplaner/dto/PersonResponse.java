package se.portplaner.dto;

import se.portplaner.model.Person;

public record PersonResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String postalCode,
        String propertyDesignation,
        String notes,
        int boatCount
) {
    public static PersonResponse from(Person p) {
        return new PersonResponse(
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getEmail(),
                p.getPhone(),
                p.getAddress(),
                p.getPostalCode(),
                p.getPropertyDesignation(),
                p.getNotes(),
                p.getBoats().size()
        );
    }
}
