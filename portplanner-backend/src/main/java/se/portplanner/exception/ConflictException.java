package se.portplanner.exception;

/** The request collides with existing data, such as an email address already in use. Gives 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
