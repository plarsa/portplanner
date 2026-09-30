package se.portplanner.dto;

/** Svar vid skapande. Engångslösenordet visas bara här och kan inte hämtas igen. */
public record UserCreatedResponse(UserResponse user, String temporaryPassword) {}
