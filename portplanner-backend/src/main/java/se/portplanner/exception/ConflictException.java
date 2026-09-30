package se.portplanner.exception;

/** Begäran krockar med befintlig data (t.ex. e-post som redan används). Ger 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
