package se.portplanner.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperAdminRegistryTest {

    private static IamProperties.SuperAdmin sa(String user, String pw, boolean enabled) {
        var s = new IamProperties.SuperAdmin();
        s.setUsername(user);
        s.setPassword(pw);
        s.setEnabled(enabled);
        return s;
    }

    private static SuperAdminRegistry registry(IamProperties.SuperAdmin... accounts) {
        var props = new IamProperties();
        props.setSuperadmins(List.of(accounts));
        var r = new SuperAdminRegistry(props);
        r.init();
        return r;
    }

    @Test
    void startFailsWithoutEnabledSuperadmin() {
        assertThrows(IllegalStateException.class, () -> registry());
        assertThrows(IllegalStateException.class, () -> registry(sa("", "", true)));
        assertThrows(IllegalStateException.class, () -> registry(sa("boss@hamn.se", "pw", false)));
    }

    @Test
    void halfFilledEntryFailsStart() {
        assertThrows(IllegalStateException.class, () -> registry(sa("boss@hamn.se", "", true)));
    }

    @Test
    void blankPlaceholderEntriesAreIgnoredWhenAnotherIsValid() {
        var r = registry(sa("", "", true), sa("boss@hamn.se", "pw", true));
        assertTrue(r.find("boss@hamn.se").isPresent());
    }

    @Test
    void plaintextIsHashedAndBcryptIsKept() {
        var enc = new BCryptPasswordEncoder();
        var hash = enc.encode("secret");
        var r = registry(sa("a@x.se", "plain", true), sa("b@x.se", "{bcrypt}" + hash, true), sa("c@x.se", hash, true));
        assertTrue(enc.matches("plain", r.find("a@x.se").orElseThrow().passwordHash()));
        assertEquals(hash, r.find("b@x.se").orElseThrow().passwordHash());
        assertEquals(hash, r.find("c@x.se").orElseThrow().passwordHash());
    }

    @Test
    void lookupIsCaseInsensitiveAndDisabledIsStillFound() {
        var r = registry(sa("Boss@Hamn.se", "pw", true), sa("old@hamn.se", "pw", false));
        assertTrue(r.find("boss@hamn.se").isPresent());
        assertFalse(r.find("old@hamn.se").orElseThrow().enabled());
        assertTrue(r.find("nobody").isEmpty());
    }

    @Test
    void duplicateUsernamesFailStart() {
        assertThrows(IllegalStateException.class,
                () -> registry(sa("a@x.se", "pw", true), sa("A@x.se", "pw", true)));
    }
}
