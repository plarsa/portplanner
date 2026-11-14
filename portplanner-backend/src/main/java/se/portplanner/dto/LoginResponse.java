package se.portplanner.dto;

import java.util.List;

/**
 * The signed-in user. {@code role} is the old single role the SPAs still use (transitional);
 * {@code roles} holds the role grants in the form {@code role:level}.
 */
public record LoginResponse(String username, String role, String email, List<String> roles,
                            List<String> loginMethods, String status, Long personId) {}
