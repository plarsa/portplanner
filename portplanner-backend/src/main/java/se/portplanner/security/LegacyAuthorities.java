package se.portplanner.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import se.portplanner.model.AppUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Övergång: de gamla enkla rollnamnen (ADMIN, HARBOUR_MASTER, MEMBER) används ännu av SecurityConfig och
 * SPA:erna. De härleds här från rolltilldelningarna och tas bort i fas 7 och 8 i iam_plan.md.
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

    /** Det gamla rollnamnet som SPA:erna förväntar sig, t.ex. {@code ADMIN}. */
    static String roleName(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.contains(":"))
                .findFirst()
                .orElse("")
                .replace("ROLE_", "");
    }
}
