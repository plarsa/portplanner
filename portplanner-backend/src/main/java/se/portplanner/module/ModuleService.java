package se.portplanner.module;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import se.portplanner.dto.ModuleResponse;
import se.portplanner.model.Role;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ModuleService {

    private static final Logger log = LoggerFactory.getLogger(ModuleService.class);

    /** Rollen som motsvarar SUPERADMIN i specen tills IAM-rollerna har migrerats. */
    static final Role SUPERADMIN_EQUIVALENT = Role.HARBOUR_MASTER;

    private final ModulesProperties props;
    private final StorageProperties storage;

    /** Beräknas en gång vid start (NFR-2). */
    private final Map<PortModule, Boolean> enabled = new EnumMap<>(PortModule.class);
    private final Map<PortModule, Map<Role, AccessLevel>> access = new EnumMap<>(PortModule.class);

    public ModuleService(ModulesProperties props, StorageProperties storage) {
        this.props = props;
        this.storage = storage;
    }

    @PostConstruct
    void init() {
        props.getModules().keySet().stream()
                .filter(id -> PortModule.find(id).isEmpty())
                .forEach(id -> log.warn("Okänd modul i konfigurationen ignoreras: portplanner.modules.{}", id));

        for (PortModule m : PortModule.values()) {
            var cfg = props.getModules().get(m.id());
            enabled.put(m, m == PortModule.BASE || (cfg != null && cfg.isEnabled()));
            var levels = new EnumMap<Role, AccessLevel>(Role.class);
            for (Role r : Role.values()) levels.put(r, AccessLevel.NONE);
            var source = cfg != null && cfg.getAccess() != null ? cfg.getAccess() : m.defaultAccess();
            levels.putAll(source);
            access.put(m, levels);
        }
        validate();
        logSummary();
    }

    private void validate() {
        var base = props.getModules().get(PortModule.BASE.id());
        if (base != null && !base.isEnabled()) {
            throw new IllegalStateException("portplanner.modules.base.enabled måste vara true: modulen 'base' är obligatorisk");
        }
        if (access.get(PortModule.BASE).get(SUPERADMIN_EQUIVALENT) != AccessLevel.WRITE) {
            throw new IllegalStateException("portplanner.modules.base.access måste ge " + SUPERADMIN_EQUIVALENT + " nivån write");
        }
        for (PortModule m : PortModule.values()) {
            if (!enabled.get(m)) continue;
            for (String depId : m.dependsOn()) {
                var dep = PortModule.of(depId);
                if (!enabled.get(dep)) {
                    throw new IllegalStateException("Modulen '" + m.id() + "' är aktiverad men beror på '" + depId
                            + "' som är avstängd. Aktivera portplanner.modules." + depId + ".enabled eller stäng av '" + m.id() + "'.");
                }
                for (Role r : Role.values()) {
                    if (level(m, r) != AccessLevel.NONE && level(dep, r) == AccessLevel.NONE) {
                        log.warn("Rollen {} har åtkomst till modulen '{}' men saknar åtkomst till '{}' som modulen beror på",
                                r, m.id(), depId);
                    }
                }
            }
        }
    }

    private void logSummary() {
        for (PortModule m : PortModule.values()) {
            log.info("Modul {}: {}{}", m.id(), enabled.get(m) ? "PÅ" : "av",
                    enabled.get(m) ? " " + access.get(m) : "");
        }
    }

    public boolean isEnabled(String id) {
        return enabled.get(PortModule.of(id));
    }

    /** Konfigurerad nivå för rollen, oavsett om modulen är aktiverad. */
    private AccessLevel level(PortModule m, Role role) {
        return access.get(m).get(role);
    }

    /** Effektiv nivå: NONE om modulen är avstängd. */
    public AccessLevel accessLevel(String id, Role role) {
        var m = PortModule.of(id);
        return enabled.get(m) && role != null ? level(m, role) : AccessLevel.NONE;
    }

    public AccessLevel accessLevel(String id, Authentication auth) {
        return accessLevel(id, roleOf(auth));
    }

    public boolean isAccessible(String id, Authentication auth) {
        return accessLevel(id, auth).atLeast(AccessLevel.READ);
    }

    /** Kastar {@link ModuleAccessException} (403) om modulen är avstängd eller nivån inte räcker. */
    public void requireAccess(String id, AccessLevel required, Authentication auth) {
        if (!isEnabled(id)) throw new ModuleAccessException(ModuleAccessException.DISABLED, id);
        if (!accessLevel(id, auth).atLeast(required)) {
            throw new ModuleAccessException(ModuleAccessException.ROLE_FORBIDDEN, id);
        }
    }

    public List<ModuleResponse> modulesFor(Authentication auth) {
        return Arrays.stream(PortModule.values()).map(m -> describe(m, auth)).toList();
    }

    public ModuleResponse moduleFor(String id, Authentication auth) {
        return describe(PortModule.of(id), auth);
    }

    private ModuleResponse describe(PortModule m, Authentication auth) {
        var lvl = accessLevel(m.id(), auth);
        boolean accessible = lvl.atLeast(AccessLevel.READ);
        return new ModuleResponse(m.id(), m.displayName(), enabled.get(m), accessible, lvl.json(),
                accessible ? publicSettings(m) : null);
    }

    /** Vitlista: bara det frontend behöver, aldrig hemligheter. */
    private Map<String, Object> publicSettings(PortModule m) {
        return switch (m) {
            case STORAGE -> Map.of("defaultSeasonStart", storage.getDefaultSeasonStart());
            default -> null;
        };
    }

    /** En roll per konto; första kända rollen används. */
    static Role roleOf(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        for (GrantedAuthority a : auth.getAuthorities()) {
            var name = a.getAuthority();
            if (!name.startsWith("ROLE_")) continue;
            try {
                return Role.valueOf(name.substring(5));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
