package se.portplanner.security;

import org.springframework.security.core.Authentication;
import se.portplanner.module.ModuleService;
import se.portplanner.module.RoleGrant;

import java.util.List;

/** Whoever performs an action: the username and role grants taken from the session. */
public record Actor(String username, List<RoleGrant> grants) {

    public static Actor from(Authentication auth) {
        return new Actor(auth.getName(), ModuleService.grantsOf(auth));
    }

    public boolean isSuperadmin() {
        return grants.stream().anyMatch(g -> g.role().equals(ModuleService.SUPERADMIN));
    }

    private boolean hasAdmin(RoleGrant.Level min) {
        return grants.stream().anyMatch(g -> g.role().equals("admin") && g.level().compareTo(min) >= 0);
    }

    /** May read the account list: superadmin, or admin with r or rw. */
    public boolean canRead() {
        return isSuperadmin() || hasAdmin(RoleGrant.Level.R);
    }

    /** May create and change accounts: superadmin or admin:rw. */
    public boolean canManage() {
        return isSuperadmin() || hasAdmin(RoleGrant.Level.RW);
    }

    public boolean is(String otherUsername) {
        return username != null && username.equalsIgnoreCase(otherUsername);
    }
}
