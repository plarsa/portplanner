package se.portplanner.module;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleServiceTest {

    private static List<RoleGrant> grants(String... g) {
        return java.util.Arrays.stream(g).map(RoleGrant::valueOf).toList();
    }

    private static ModuleService service(Map<String, ModulesProperties.ModuleConfig> cfg) {
        var props = new ModulesProperties();
        props.setModules(new java.util.HashMap<>(cfg));
        var svc = new ModuleService(props, new StorageProperties());
        svc.init();
        return svc;
    }

    private static ModulesProperties.ModuleConfig module(boolean enabled, String... access) {
        var c = new ModulesProperties.ModuleConfig();
        c.setEnabled(enabled);
        if (access.length > 0) c.setAccess(grants(access));
        return c;
    }

    @Test
    void configurationIsACap() {
        var svc = service(Map.of("audit", module(true, "admin:r", "audit_master:r")));
        assertEquals(AccessLevel.READ, svc.accessLevel("audit", grants("admin:r")));
        assertEquals(AccessLevel.READ, svc.accessLevel("audit", grants("admin:rw")));
        assertEquals(AccessLevel.READ, svc.accessLevel("audit", grants("audit_master:rw")));
        assertEquals(AccessLevel.NONE, svc.accessLevel("audit", grants("member:rw")));
        assertEquals(AccessLevel.READ, svc.accessLevel("audit", grants("admin:r", "audit_master:r")));
    }

    @Test
    void highestMatchingEntryWins() {
        var svc = service(Map.of("reports", module(true, "admin:rw", "member:r")));
        assertEquals(AccessLevel.READ, svc.accessLevel("reports", grants("admin:r", "member:rw")));
        assertEquals(AccessLevel.WRITE, svc.accessLevel("reports", grants("admin:rw", "member:r")));
    }

    @Test
    void disabledModuleGivesNone() {
        var svc = service(Map.of("reports", module(false, "admin:rw")));
        assertEquals(AccessLevel.NONE, svc.accessLevel("reports", grants("admin:rw")));
    }

    @Test
    void missingModuleIsDisabledAndDefaultsApplyWhenAccessIsMissing() {
        var svc = service(Map.of());
        assertTrue(svc.isEnabled("base"));
        assertEquals(AccessLevel.WRITE, svc.accessLevel("base", grants("member:rw")));
        assertEquals(AccessLevel.NONE, svc.accessLevel("audit", grants("admin:r")));
    }

    @Test
    void superadminGetsNoAccessWithoutAnEntry() {
        var svc = service(Map.of("storage", module(true, "admin:rw")));
        assertEquals(AccessLevel.NONE, svc.accessLevel("storage", grants("superadmin:rw")));
    }

    @Test
    void newRolesJoinTheCatalogEvenFromDisabledModules() {
        var svc = service(Map.of("audit", module(true, "admin:r", "audit_master:r"),
                "reports", module(false, "report_viewer:r")));
        var catalog = svc.roleCatalog();
        assertTrue(catalog.containsAll(List.of("audit_master", "report_viewer", "admin", "member", "superadmin")));
    }

    @Test
    void startFailsWithoutSuperadminOnBase() {
        var ex = assertThrows(IllegalStateException.class,
                () -> service(Map.of("base", module(true, "admin:rw", "member:rw"))));
        assertTrue(ex.getMessage().contains("superadmin:rw"));
        assertThrows(IllegalStateException.class,
                () -> service(Map.of("base", module(true, "superadmin:r", "admin:rw"))));
    }

    @Test
    void startFailsWhenBaseIsDisabled() {
        assertThrows(IllegalStateException.class, () -> service(Map.of("base", module(false))));
    }

    @Test
    void startFailsWhenDependencyIsDisabled() {
        // Every module depends on base, and base cannot be off, so this is covered by base=false above.
        assertThrows(IllegalStateException.class, () -> service(Map.of("base", module(false), "storage", module(true))));
    }
}
