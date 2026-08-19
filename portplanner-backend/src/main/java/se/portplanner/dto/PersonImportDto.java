package se.portplanner.dto;

public record PersonImportDto(
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String postalCode,
        String propertyDesignation,
        String notes
) {}
