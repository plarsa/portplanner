package se.portplaner.dto;

public record PersonImportDto(
        String firstName,
        String lastName,
        String email,
        String phone
) {}
