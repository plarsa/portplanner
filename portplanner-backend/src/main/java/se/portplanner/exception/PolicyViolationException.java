package se.portplanner.exception;

/** En regel för kontohantering har brutits. Ger 403 med en maskinläsbar kod. */
public class PolicyViolationException extends RuntimeException {

    private final String code;

    public PolicyViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
