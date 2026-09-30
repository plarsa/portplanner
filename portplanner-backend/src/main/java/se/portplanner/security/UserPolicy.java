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

/** Regler för vem som får göra vad med konton (ROL-4, ROL-5, ROL-10, KON-6, KON-7, LOG-13, LOG-14). */
@Component
public class UserPolicy {

    /** Metoder som är aktiverade i systemet. Fler läggs till när de byggs (portplanner.iam.&lt;metod&gt;.enabled). */
    private static final Set<LoginMethod> ENABLED_METHODS = EnumSet.of(LoginMethod.PASSWORD);

    private final Supplier<Set<String>> moduleRoles;

    @Autowired
    public UserPolicy(ModuleService modules) {
        this(modules::roleCatalog);
    }

    UserPolicy(Supplier<Set<String>> moduleRoles) {
        this.moduleRoles = moduleRoles;
    }

    /** Roller som går att tilldela: modulernas roller plus admin och member, aldrig superadmin. */
    public Set<String> assignableRoles() {
        var roles = new TreeSet<>(moduleRoles.get());
        roles.add("admin");
        roles.add("member");
        roles.remove(ModuleService.SUPERADMIN);
        return roles;
    }

    public void assertCanRead(Actor actor) {
        if (!actor.canRead()) {
            throw new PolicyViolationException("ACCOUNT_ACCESS_FORBIDDEN", "Saknar behörighet till kontohantering");
        }
    }

    public void assertCanManage(Actor actor) {
        if (!actor.canManage()) {
            throw new PolicyViolationException("ACCOUNT_ACCESS_FORBIDDEN", "Saknar behörighet att ändra konton");
        }
    }

    /** Kontroller som gäller både vid skapande och ändring av rolltilldelningar. */
    public void assertRolesAssignable(Actor actor, Collection<RoleGrant> roles) {
        assertCanManage(actor);
        var assignable = assignableRoles();
        for (RoleGrant g : roles) {
            if (g.role().equals(ModuleService.SUPERADMIN)) {
                throw new PolicyViolationException("ROLE_ASSIGNMENT_FORBIDDEN",
                        "Rollen superadmin kan bara definieras i konfigurationen");
            }
            if (!assignable.contains(g.role())) {
                throw new IllegalArgumentException("Okänd roll: " + g.role());
            }
        }
    }

    public void assertCanModify(Actor actor, AppUser target, Collection<RoleGrant> newRoles) {
        assertCanManage(actor);
        assertRolesAssignable(actor, newRoles);
        if (actor.is(target.getUsername()) && !normalise(newRoles).equals(target.getRoles())) {
            throw new PolicyViolationException("SELF_MODIFICATION", "Du kan inte ändra dina egna roller");
        }
    }

    /** Samma normalisering som {@link AppUser#setRoles}: en post per roll, högsta nivån gäller. */
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
            throw new IllegalArgumentException("Ett aktivt konto måste ha minst en inloggningsmetod");
        }
        for (var m : methods) {
            if (!ENABLED_METHODS.contains(m)) {
                throw new IllegalArgumentException("Inloggningsmetoden " + m + " är inte aktiverad");
            }
        }
    }
}
