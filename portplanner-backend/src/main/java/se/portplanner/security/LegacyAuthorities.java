package se.portplanner.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import se.portplanner.model.AppUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Transitional: the old single role names (ADMIN, HARBOUR_MASTER, MEMBER) are still used by SecurityConfig
 * and the SPAs. They are derived here from the role grants and go away in phases 7 and 8 of iam_plan.md.
 */
final class LegacyAuthorities {

    static final String ADMIN = "ROLE_ADMIN";
    static final String SUPERADMIN = "ROLE_HARBOUR_MASTER";
    static final String MEMBER = "ROLE_MEMBER";

    private LegacyAuthorities() {}

    static List<GrantedAuthority> of(AppUser user) {
        var list = new ArrayList<GrantedAuthority>();
        user.getRoles().forEach(g -> list.add(new SimpleGrantedAuthority(g.toAuthority())));
        boolean nonMember = user.getRoles().stream().anyMatch(g -> !g.role().equals("member"));
        if (nonMember) list.add(new SimpleGrantedAuthority(ADMIN));
        if (user.hasRole("member")) list.add(new SimpleGrantedAuthority(MEMBER));
        return list;
    }

    /** The old role name the SPAs expect, for example {@code ADMIN}. */
    static String roleName(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.contains(":"))
                .findFirst()
                .orElse("")
                .replace("ROLE_", "");
    }
}
