package se.portplanner.module;

public class ModuleAccessException extends RuntimeException {

    public static final String DISABLED = "MODULE_DISABLED";
    public static final String ROLE_FORBIDDEN = "MODULE_ROLE_FORBIDDEN";

    private final String code;
    private final String module;

    public ModuleAccessException(String code, String module) {
        super(code + ": " + module);
        this.code = code;
        this.module = module;
    }

    public String getCode() { return code; }
    public String getModule() { return module; }
}
