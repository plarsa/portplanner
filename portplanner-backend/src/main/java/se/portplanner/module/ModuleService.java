package se.portplanner.module;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import se.portplanner.dto.ModuleResponse;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class ModuleService {

    private static final Logger log = LoggerFactory.getLogger(ModuleService.class);

    public static final String SUPERADMIN = "superadmin";

    private final ModulesProperties props;
    private final StorageProperties storage;

    /** Computed once at startup (NFR-2). */
    private final Map<PortModule, Boolean> enabled = new EnumMap<>(PortModule.class);
    private final Map<PortModule, List<RoleGrant>> access = new EnumMap<>(PortModule.class);

    public ModuleService(ModulesProperties props, StorageProperties storage) {
        this.props = props;
        this.storage = storage;
    }

    @PostConstruct
    void init() {
        props.getModules().keySet().stream()
                .filter(id -> PortModule.find(id).isEmpty())
                .forEach(id -> log.warn("Ignoring unknown module in the configuration: portplanner.modules.{}", id));

        for (PortModule m : PortModule.values()) {
            var cfg = props.getModules().get(m.id());
            enabled.put(m, m == PortModule.BASE || (cfg != null && cfg.isEnabled()));
            access.put(m, cfg != null && cfg.getAccess() != null ? List.copyOf(cfg.getAccess()) : m.defaultAccess());
        }
        validate();
        logSummary();
    }

    private void validate() {
        var base = props.getModules().get(PortModule.BASE.id());
        if (base != null && !base.isEnabled()) {
            throw new IllegalStateException("portplanner.modules.base.enabled must be true: the 'base' module is mandatory");
        }
        if (!access.get(PortModule.BASE).contains(new RoleGrant(SUPERADMIN, RoleGrant.Level.RW))) {
            throw new IllegalStateException("portplanner.modules.base.access must contain " + SUPERADMIN + ":rw");
        }
        for (PortModule m : PortModule.values()) {
            if (!enabled.get(m)) continue;
            for (String depId : m.dependsOn()) {
                var dep = PortModule.of(depId);
                if (!enabled.get(dep)) {
                    throw new IllegalStateException("Module '" + m.id() + "' is enabled but depends on '" + depId
                            + "' which is off. Enable portplanner.modules." + depId + ".enabled or turn off '" + m.id() + "'.");
                }
                var depRoles = access.get(dep).stream().map(RoleGrant::role).collect(java.util.stream.Collectors.toSet());
                for (RoleGrant g : access.get(m)) {
                    if (!depRoles.contains(g.role())) {
                        log.warn("Role {} is in the access list of module '{}' but missing from '{}', which it depends on",
                                g.role(), m.id(), depId);
                    }
                }
            }
        }
    }

    private void logSummary() {
        for (PortModule m : PortModule.values()) {
            log.info("Module {}: {}{}", m.id(), enabled.get(m) ? "on" : "off",
                    enabled.get(m) ? " access=" + access.get(m) : "");
        }
        log.info("Role catalogue: {}", roleCatalog());
    }

    public boolean isEnabled(String id) {
        return enabled.get(PortModule.of(id));
    }

    /**
     * The resulting level for a set of role grants. Every entry in the module's access list is a ceiling: the
     * level is the lower of the account's and the entry's, and the highest wins when several match. A module
     * that is off gives NONE.
     */
    public AccessLevel accessLevel(String id, Collection<RoleGrant> grants) {
        var m = PortModule.of(id);
        if (!enabled.get(m)) return AccessLevel.NONE;
        RoleGrant.Level best = null;
        for (RoleGrant entry : access.get(m)) {
            for (RoleGrant g : grants) {
                if (g.role().equals(entry.role())) {
                    var level = RoleGrant.Level.min(g.level(), entry.level());
                    best = best == null ? level : RoleGrant.Level.max(best, level);
                }
            }
        }
        return best == null ? AccessLevel.NONE : best.toAccessLevel();
    }

    public AccessLevel accessLevel(String id, Authentication auth) {
        return accessLevel(id, grantsOf(auth));
    }

    public boolean isAccessible(String id, Authentication auth) {
        return accessLevel(id, auth).atLeast(AccessLevel.READ);
    }

    /** Every role name that appears in any module's access list, including modules that are off. */
    public Set<String> roleCatalog() {
        var roles = new TreeSet<String>();
        access.values().forEach(list -> list.forEach(g -> roles.add(g.role())));
        return roles;
    }

    /** Throws {@link ModuleAccessException} (403) when the module is off or the level is not enough. */
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

    /** An allow list: only what the frontend needs, never secrets. */
    private Map<String, Object> publicSettings(PortModule m) {
        return switch (m) {
            case STORAGE -> Map.of("defaultSeasonStart", storage.getDefaultSeasonStart());
            default -> null;
        };
    }

    /** The account's role grants, read from authorities of the form {@code ROLE_role:level}. */
    public static List<RoleGrant> grantsOf(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return List.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .flatMap(a -> RoleGrant.fromAuthority(a).stream())
                .toList();
    }
}
