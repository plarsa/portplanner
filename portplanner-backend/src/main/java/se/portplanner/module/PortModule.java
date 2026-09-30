package se.portplanner.module;


import java.util.Arrays;
import java.util.List;
import java.util.Optional;


/** Register över kända moduler. Standardposterna används när {@code access} saknas i konfigurationen. */
public enum PortModule {
    BASE("base", "Bas", List.of(), grants("superadmin:rw", "admin:rw", "member:rw")),
    STORAGE("storage", "Uppläggningsplatser", List.of("base"), grants("superadmin:rw", "admin:rw", "member:rw")),
    MEMBER_PORTAL("member-portal", "Medlemssidor", List.of("base"), grants("superadmin:rw", "member:rw")),
    AUDIT("audit", "Händelseloggning", List.of("base"), grants("superadmin:r", "admin:r")),
    REPORTS("reports", "Rapporter", List.of("base"), grants("superadmin:rw", "admin:r")),
    AI_MAIL("ai-mail", "AI-mailhantering", List.of("base"), grants("superadmin:rw", "admin:rw")),
    IMPORT_EXPORT("import-export", "Import/export", List.of("base"), grants("superadmin:rw", "admin:rw"));

    private final String id;
    private final String displayName;
    private final List<String> dependsOn;
    private final List<RoleGrant> defaultAccess;

    PortModule(String id, String displayName, List<String> dependsOn, List<RoleGrant> defaultAccess) {
        this.id = id;
        this.displayName = displayName;
        this.dependsOn = dependsOn;
        this.defaultAccess = defaultAccess;
    }

    private static List<RoleGrant> grants(String... entries) {
        return Arrays.stream(entries).map(RoleGrant::valueOf).toList();
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public List<String> dependsOn() { return dependsOn; }
    public List<RoleGrant> defaultAccess() { return defaultAccess; }

    public static Optional<PortModule> find(String id) {
        return Arrays.stream(values()).filter(m -> m.id.equals(id)).findFirst();
    }

    public static PortModule of(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Okänd modul: " + id));
    }
}
