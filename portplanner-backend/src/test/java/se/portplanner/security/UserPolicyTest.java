package se.portplanner.security;

import org.junit.jupiter.api.Test;
import se.portplanner.exception.PolicyViolationException;
import se.portplanner.model.AppUser;
import se.portplanner.model.LoginMethod;
import se.portplanner.model.UserStatus;
import se.portplanner.module.RoleGrant;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserPolicyTest {

    private final UserPolicy policy = new UserPolicy(() -> Set.of("admin", "member", "audit_master", "superadmin"));

    private static Actor actor(String name, String... grants) {
        return new Actor(name, java.util.Arrays.stream(grants).map(RoleGrant::valueOf).toList());
    }

    private static List<RoleGrant> roles(String... g) {
        return java.util.Arrays.stream(g).map(RoleGrant::valueOf).toList();
    }

    private static AppUser account(String email, String... grants) {
        var u = new AppUser();
        u.setEmail(email);
        u.setRoles(roles(grants));
        return u;
    }

    private static String code(Runnable r) {
        return assertThrows(PolicyViolationException.class, r::run).getCode();
    }

    @Test
    void readAndManageRights() {
        var superadmin = actor("s", "superadmin:rw");
        var adminRw = actor("a", "admin:rw");
        var adminR = actor("r", "admin:r");
        var member = actor("m", "member:rw");
        var auditMaster = actor("x", "audit_master:rw");

        assertTrue(superadmin.canManage() && adminRw.canManage());
        assertTrue(adminR.canRead());
        assertFalse(adminR.canManage());
        assertFalse(member.canRead());
        assertFalse(auditMaster.canRead());

        policy.assertCanRead(adminR);
        assertEquals("ACCOUNT_ACCESS_FORBIDDEN", code(() -> policy.assertCanManage(adminR)));
        assertEquals("ACCOUNT_ACCESS_FORBIDDEN", code(() -> policy.assertCanRead(member)));
    }

    @Test
    void assignableRolesComeFromModulesAndNeverIncludeSuperadmin() {
        assertEquals(Set.of("admin", "member", "audit_master"), policy.assignableRoles());
    }

    @Test
    void superadminCanNeverBeAssigned() {
        var admin = actor("a", "admin:rw");
        assertEquals("ROLE_ASSIGNMENT_FORBIDDEN",
                code(() -> policy.assertRolesAssignable(admin, roles("superadmin:rw"))));
        assertEquals("ROLE_ASSIGNMENT_FORBIDDEN",
                code(() -> policy.assertRolesAssignable(actor("s", "superadmin:rw"), roles("superadmin:rw"))));
    }

    @Test
    void unknownRolesAreRejectedAndKnownOnesAccepted() {
        var admin = actor("a", "admin:rw");
        assertThrows(IllegalArgumentException.class, () -> policy.assertRolesAssignable(admin, roles("finns_inte:r")));
        policy.assertRolesAssignable(admin, roles("admin:r", "audit_master:rw", "member:rw"));
    }

    @Test
    void nobodyChangesTheirOwnRoles() {
        var me = account("a@hamn.se", "admin:rw");
        var actor = actor("a@hamn.se", "admin:rw");
        assertEquals("SELF_MODIFICATION", code(() -> policy.assertCanModify(actor, me, roles("admin:rw", "audit_master:r"))));
        assertEquals("SELF_MODIFICATION", code(() -> policy.assertCanModify(actor, me, roles("admin:r"))));
        // Oförändrade roller är tillåtet (t.ex. byte av person eller e-post).
        policy.assertCanModify(actor, me, roles("admin:rw"));
        policy.assertCanModify(actor, me, roles("admin:r", "admin:rw"));
    }

    @Test
    void adminCanChangeOthers() {
        var other = account("b@hamn.se", "member:rw");
        policy.assertCanModify(actor("a@hamn.se", "admin:rw"), other, roles("member:rw", "audit_master:r"));
        assertEquals("ACCOUNT_ACCESS_FORBIDDEN",
                code(() -> policy.assertCanModify(actor("r@hamn.se", "admin:r"), other, roles("member:rw"))));
    }

    @Test
    void nobodyDisablesThemselves() {
        var me = account("a@hamn.se", "admin:rw");
        assertEquals("SELF_MODIFICATION",
                code(() -> policy.assertCanChangeStatus(actor("a@hamn.se", "admin:rw"), me, UserStatus.DISABLED)));
        policy.assertCanChangeStatus(actor("x@hamn.se", "admin:rw"), me, UserStatus.DISABLED);
        policy.assertCanChangeStatus(actor("a@hamn.se", "admin:rw"), me, UserStatus.ACTIVE);
    }

    @Test
    void loginMethodListMustBeNonEmptyForActiveAccounts() {
        policy.assertValidLoginMethods(Set.of(LoginMethod.PASSWORD), UserStatus.ACTIVE);
        assertThrows(IllegalArgumentException.class, () -> policy.assertValidLoginMethods(Set.of(), UserStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () -> policy.assertValidLoginMethods(Set.of(), UserStatus.INVITED));
        policy.assertValidLoginMethods(Set.of(), UserStatus.DISABLED);
    }
}
