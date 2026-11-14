package se.portplanner.exception;

/** An account management rule was broken. Gives 403 with a machine-readable code. */
public class PolicyViolationException extends RuntimeException {

    private final String code;

    public PolicyViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
