package se.portplanner.module;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleGrantTest {

    @Test
    void parsesRoleAndLevel() {
        assertEquals(new RoleGrant("admin", RoleGrant.Level.RW), RoleGrant.valueOf("admin:rw"));
        assertEquals(new RoleGrant("audit_master", RoleGrant.Level.R), RoleGrant.valueOf("audit_master:r"));
        assertEquals("admin:rw", RoleGrant.valueOf("admin:rw").toString());
    }

    @Test
    void rejectsInvalidEntries() {
        for (var bad : new String[] {"admin", "admin:", ":rw", "admin:write", "Admin:rw", "1admin:rw", "ad min:rw", ""}) {
            assertThrows(IllegalArgumentException.class, () -> RoleGrant.valueOf(bad), bad);
        }
    }

    @Test
    void roundTripsThroughAuthority() {
        var g = RoleGrant.valueOf("member:rw");
        assertEquals("ROLE_member:rw", g.toAuthority());
        assertEquals(g, RoleGrant.fromAuthority("ROLE_member:rw").orElseThrow());
        assertTrue(RoleGrant.fromAuthority("ROLE_ADMIN").isEmpty());
        assertTrue(RoleGrant.fromAuthority("other").isEmpty());
    }
}
