package se.portplanner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Skapa eller ersätta ett konto. {@code username} anges inte: det följer alltid e-postadressen.
 * {@code roles} skrivs {@code roll:nivå} (t.ex. {@code admin:rw}). {@code loginMethods} utelämnat betyder
 * PASSWORD vid skapande och oförändrat vid ändring. {@code personId} null betyder ingen kopplad person.
 */
public record UserRequest(
        @NotBlank @Email String email,
        @NotNull List<String> roles,
        List<String> loginMethods,
        Long personId
) {}
