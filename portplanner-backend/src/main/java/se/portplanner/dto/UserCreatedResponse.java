package se.portplanner.dto;

/** The response to account creation. The one-time password appears only here and cannot be fetched again. */
public record UserCreatedResponse(UserResponse user, String temporaryPassword) {}
