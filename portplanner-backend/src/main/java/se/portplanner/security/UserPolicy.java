package se.portplanner.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.portplanner.exception.PolicyViolationException;
import se.portplanner.model.AppUser;
import se.portplanner.model.LoginMethod;
import se.portplanner.model.UserStatus;
import se.portplanner.module.ModuleService;
import se.portplanner.module.RoleGrant;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/** The rules for who may do what with accounts (ROL-4, ROL-5, ROL-10, KON-6, KON-7, LOG-13, LOG-14). */
@Component
public class UserPolicy {

    /** The login methods enabled in the system. More are added here as they are built (portplanner.iam.&lt;method&gt;.enabled). */
    private static final Set<LoginMethod> ENABLED_METHODS = EnumSet.of(LoginMethod.PASSWORD);

    private final Supplier<Set<String>> moduleRoles;

    @Autowired
    public UserPolicy(ModuleService modules) {
        this(modules::roleCatalog);
    }

    UserPolicy(Supplier<Set<String>> moduleRoles) {
        this.moduleRoles = moduleRoles;
    }

    /** The roles that can be assigned: the modules' roles plus admin and member, never superadmin. */
    public Set<String> assignableRoles() {
        var roles = new TreeSet<>(moduleRoles.get());
        roles.add("admin");
        roles.add("member");
        roles.remove(ModuleService.SUPERADMIN);
        return roles;
    }

    public void assertCanRead(Actor actor) {
        if (!actor.canRead()) {
            throw new PolicyViolationException("ACCOUNT_ACCESS_FORBIDDEN", "Not allowed to manage accounts");
        }
    }

    public void assertCanManage(Actor actor) {
        if (!actor.canManage()) {
            throw new PolicyViolationException("ACCOUNT_ACCESS_FORBIDDEN", "Not allowed to change accounts");
        }
    }

    /** The checks that apply both when creating and when changing role grants. */
    public void assertRolesAssignable(Actor actor, Collection<RoleGrant> roles) {
        assertCanManage(actor);
        var assignable = assignableRoles();
        for (RoleGrant g : roles) {
            if (g.role().equals(ModuleService.SUPERADMIN)) {
                throw new PolicyViolationException("ROLE_ASSIGNMENT_FORBIDDEN",
                        "Rollen superadmin kan bara definieras i konfigurationen");
            }
            if (!assignable.contains(g.role())) {
                throw new IllegalArgumentException("Unknown role: " + g.role());
            }
        }
    }

    public void assertCanModify(Actor actor, AppUser target, Collection<RoleGrant> newRoles) {
        assertCanManage(actor);
        assertRolesAssignable(actor, newRoles);
        if (actor.is(target.getUsername()) && !normalise(newRoles).equals(target.getRoles())) {
            throw new PolicyViolationException("SELF_MODIFICATION", "You cannot change your own roles");
        }
    }

    /** The same normalization as {@link AppUser#setRoles}: one entry per role, the highest level wins. */
    public static Set<RoleGrant> normalise(Collection<RoleGrant> grants) {
        var copy = new AppUser();
        copy.setRoles(grants);
        return copy.getRoles();
    }

    public void assertCanChangeStatus(Actor actor, AppUser target, UserStatus newStatus) {
        assertCanManage(actor);
        if (newStatus == UserStatus.DISABLED && actor.is(target.getUsername())) {
            throw new PolicyViolationException("SELF_MODIFICATION", "Du kan inte inaktivera ditt eget konto");
        }
    }

    public void assertValidLoginMethods(Collection<LoginMethod> methods, UserStatus status) {
        if (status != UserStatus.DISABLED && methods.isEmpty()) {
            throw new IllegalArgumentException("An active account must have at least one login method");
        }
        for (var m : methods) {
            if (!ENABLED_METHODS.contains(m)) {
                throw new IllegalArgumentException("Login method " + m + " is not enabled");
            }
        }
    }
}
