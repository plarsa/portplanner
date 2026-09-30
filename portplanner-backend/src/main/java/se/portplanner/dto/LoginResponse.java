package se.portplanner.dto;

import java.util.List;

/**
 * Inloggad användare. {@code role} är den gamla enkelrollen som SPA:erna ännu använder (övergång);
 * {@code roles} är rolltilldelningarna på formen {@code roll:nivå}.
 */
public record LoginResponse(String username, String role, String email, List<String> roles,
                            List<String> loginMethods, String status, Long personId) {}
