package se.portplanner.module;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;

/** Testkonton med rolltilldelningar på formen {@code roll:nivå}. */
public final class TestUsers {

    private TestUsers() {}

    public static RequestPostProcessor asUser(String... grants) {
        return asUserNamed("u", grants);
    }

    public static RequestPostProcessor asUserNamed(String username, String... grants) {
        var authorities = Arrays.stream(grants)
                .map(g -> new SimpleGrantedAuthority(RoleGrant.valueOf(g).toAuthority()))
                .toList();
        return SecurityMockMvcRequestPostProcessors.user(username).authorities(authorities);
    }
}
