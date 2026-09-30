package se.portplanner.module;

import se.portplanner.model.Role;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static se.portplanner.model.Role.ADMIN;
import static se.portplanner.model.Role.HARBOUR_MASTER;
import static se.portplanner.model.Role.MEMBER;
import static se.portplanner.module.AccessLevel.NONE;
import static se.portplanner.module.AccessLevel.READ;
import static se.portplanner.module.AccessLevel.WRITE;

/**
 * Register över kända moduler. Standardnivåerna används när {@code access} saknas i konfigurationen.
 * HARBOUR_MASTER motsvarar SUPERADMIN i specen tills rollerna i IAM har migrerats.
 */
public enum PortModule {
    BASE("base", "Bas", List.of(), levels(WRITE, WRITE, WRITE)),
    STORAGE("storage", "Uppläggningsplatser", List.of("base"), levels(WRITE, WRITE, WRITE)),
    MEMBER_PORTAL("member-portal", "Medlemssidor", List.of("base"), levels(WRITE, NONE, WRITE)),
    AUDIT("audit", "Händelseloggning", List.of("base"), levels(READ, READ, NONE)),
    REPORTS("reports", "Rapporter", List.of("base"), levels(WRITE, READ, NONE)),
    AI_MAIL("ai-mail", "AI-mailhantering", List.of("base"), levels(WRITE, WRITE, NONE)),
    IMPORT_EXPORT("import-export", "Import/export", List.of("base"), levels(WRITE, WRITE, NONE));

    private final String id;
    private final String displayName;
    private final List<String> dependsOn;
    private final Map<Role, AccessLevel> defaultAccess;

    PortModule(String id, String displayName, List<String> dependsOn, Map<Role, AccessLevel> defaultAccess) {
        this.id = id;
        this.displayName = displayName;
        this.dependsOn = dependsOn;
        this.defaultAccess = defaultAccess;
    }

    /** Ordning: superadmin-motsvarighet, ADMIN, MEMBER. */
    private static Map<Role, AccessLevel> levels(AccessLevel superadmin, AccessLevel admin, AccessLevel member) {
        return Map.of(HARBOUR_MASTER, superadmin, ADMIN, admin, MEMBER, member);
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public List<String> dependsOn() { return dependsOn; }
    public Map<Role, AccessLevel> defaultAccess() { return defaultAccess; }

    public static Optional<PortModule> find(String id) {
        return Arrays.stream(values()).filter(m -> m.id.equals(id)).findFirst();
    }

    public static PortModule of(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Okänd modul: " + id));
    }
}
