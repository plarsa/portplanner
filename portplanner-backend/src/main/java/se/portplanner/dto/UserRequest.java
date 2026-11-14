package se.portplanner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Creates or replaces an account. {@code username} is not given: it always follows the email address.
 * {@code roles} are written {@code role:level} (for example {@code admin:rw}). Leaving out {@code loginMethods}
 * means PASSWORD on creation and no change on update. A null {@code personId} means no linked person.
 */
public record UserRequest(
        @NotBlank @Email String email,
        @NotNull List<String> roles,
        List<String> loginMethods,
        Long personId
) {}
