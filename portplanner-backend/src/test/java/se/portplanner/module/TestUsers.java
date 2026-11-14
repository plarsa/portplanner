package se.portplanner.module;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Test accounts with role grants in the form {@code role:level}. The transitional single-role
 * authorities are derived the same way {@code LegacyAuthorities.of} derives them for a real
 * account, so a test user reaches the endpoints that still gate on ROLE_ADMIN or ROLE_MEMBER.
 */
public final class TestUsers {

    private TestUsers() {}

    public static RequestPostProcessor asUser(String... grants) {
        return asUserNamed("u", grants);
    }

    public static RequestPostProcessor asUserNamed(String username, String... grants) {
        var parsed = Arrays.stream(grants).map(RoleGrant::valueOf).toList();
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        parsed.forEach(g -> authorities.add(new SimpleGrantedAuthority(g.toAuthority())));
        if (parsed.stream().anyMatch(g -> g.role().equals("superadmin"))) {
            // A configured superadmin is issued ROLE_HARBOUR_MASTER and never ROLE_ADMIN,
            // the way UserDetailsServiceImpl issues it.
            authorities.add(new SimpleGrantedAuthority("ROLE_HARBOUR_MASTER"));
        }
        if (parsed.stream().anyMatch(g -> !g.role().equals("member") && !g.role().equals("superadmin"))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (parsed.stream().anyMatch(g -> g.role().equals("member"))) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
        }
        return SecurityMockMvcRequestPostProcessors.user(username).authorities(authorities);
    }
}
