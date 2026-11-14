package se.portplanner.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.UserCreatedResponse;
import se.portplanner.dto.UserRequest;
import se.portplanner.dto.UserResponse;
import se.portplanner.exception.ConflictException;
import se.portplanner.exception.ResourceNotFoundException;
import se.portplanner.model.AppUser;
import se.portplanner.model.LoginMethod;
import se.portplanner.model.UserStatus;
import se.portplanner.module.ModuleService;
import se.portplanner.module.RoleGrant;
import se.portplanner.repository.AppUserRepository;
import se.portplanner.repository.PersonRepository;
import se.portplanner.security.Actor;
import se.portplanner.security.PasswordGenerator;
import se.portplanner.security.SessionInvalidator;
import se.portplanner.security.SuperAdminRegistry;
import se.portplanner.security.UserPolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final AppUserRepository users;
    private final PersonRepository persons;
    private final UserPolicy policy;
    private final ModuleService modules;
    private final SuperAdminRegistry superAdmins;
    private final PasswordEncoder encoder;
    private final PasswordGenerator passwords;
    private final SessionInvalidator sessions;
    private final AuditService audit;

    public UserService(AppUserRepository users, PersonRepository persons, UserPolicy policy, ModuleService modules,
                       SuperAdminRegistry superAdmins, PasswordEncoder encoder, PasswordGenerator passwords,
                       SessionInvalidator sessions, AuditService audit) {
        this.users = users;
        this.persons = persons;
        this.policy = policy;
        this.modules = modules;
        this.superAdmins = superAdmins;
        this.encoder = encoder;
        this.passwords = passwords;
        this.sessions = sessions;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<String> assignableRoles(Actor actor) {
        policy.assertCanManage(actor);
        return List.copyOf(policy.assignableRoles());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(Actor actor, String role, String status) {
        policy.assertCanRead(actor);
        var known = knownRoles();
        var result = new ArrayList<UserResponse>();
        superAdmins.all().forEach(sa -> result.add(UserResponse.configuredSuperadmin(sa.username(), sa.enabled())));
        users.findAll().stream()
                .map(u -> UserResponse.from(u, known))
                .forEach(result::add);
        return result.stream()
                .filter(u -> status == null || status.isBlank() || u.status().equalsIgnoreCase(status))
                .filter(u -> role == null || role.isBlank() || u.roles().stream().anyMatch(r -> r.startsWith(role + ":")))
                .sorted(Comparator.comparing(UserResponse::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Actor actor, Long id) {
        policy.assertCanRead(actor);
        return UserResponse.from(getOrThrow(id), knownRoles());
    }

    public UserCreatedResponse create(Actor actor, UserRequest req) {
        policy.assertCanManage(actor);
        var grants = parseRoles(req.roles());
        policy.assertRolesAssignable(actor, grants);
        var methods = req.loginMethods() == null ? Set.of(LoginMethod.PASSWORD) : parseMethods(req.loginMethods());
        policy.assertValidLoginMethods(methods, UserStatus.INVITED);
        assertEmailFree(req.email(), null);

        var user = new AppUser();
        user.setEmail(req.email().trim());
        user.setRoles(grants);
        user.setLoginMethods(new HashSet<>(methods));
        applyPerson(user, req.personId());
        String temporary = passwords.generate();
        user.setPasswordHash(encoder.encode(temporary));
        user.setStatus(UserStatus.INVITED);
        user.setMustChangePassword(true);
        user = users.save(user);

        audit.log("USER_CREATED", "USER", user.getId(),
                "Account created: " + user.getUsername() + ", roles " + rolesText(user));
        return new UserCreatedResponse(UserResponse.from(user, knownRoles()), temporary);
    }

    public UserResponse update(Actor actor, Long id, UserRequest req) {
        var user = getOrThrow(id);
        var grants = parseRoles(req.roles());
        policy.assertCanModify(actor, user, grants);
        var methods = req.loginMethods() == null ? user.getLoginMethods() : parseMethods(req.loginMethods());
        policy.assertValidLoginMethods(methods, user.getStatus());

        String oldUsername = user.getUsername();
        String oldRoles = rolesText(user);
        var oldMethods = new HashSet<>(user.getLoginMethods());
        boolean emailChanged = !req.email().trim().equalsIgnoreCase(user.getEmail() == null ? "" : user.getEmail());
        if (emailChanged || user.getEmail() == null) assertEmailFree(req.email(), user);

        boolean rolesChanged = !UserPolicy.normalise(grants).equals(user.getRoles());
        boolean methodsChanged = !new HashSet<>(methods).equals(oldMethods);

        user.setEmail(req.email().trim());
        user.setRoles(grants);
        user.setLoginMethods(new HashSet<>(methods));
        applyPerson(user, req.personId());
        user = users.save(user);

        if (emailChanged) {
            audit.log("USER_EMAIL_CHANGED", "USER", user.getId(), "Email changed: " + oldUsername + " -> " + user.getUsername());
        }
        if (rolesChanged) {
            audit.log("USER_ROLES_CHANGED", "USER", user.getId(),
                    "Roles for " + user.getUsername() + ": " + oldRoles + " -> " + rolesText(user));
        }
        if (methodsChanged) {
            audit.log("USER_METHODS_CHANGED", "USER", user.getId(),
                    "Login methods for " + user.getUsername() + ": " + oldMethods + " -> " + user.getLoginMethods());
        }
        if (emailChanged || rolesChanged || methodsChanged) {
            sessions.invalidate(oldUsername);
        }
        return UserResponse.from(user, knownRoles());
    }

    /**
     * Issues a new generated password and returns it once, the way account creation does. The
     * status is left alone: a reset is not an invitation, and a disabled account stays disabled
     * until it is re-enabled. Existing sessions end, since a reset is also what an administrator
     * reaches for when a password may be in the wrong hands.
     */
    public UserCreatedResponse resetPassword(Actor actor, Long id) {
        var user = getOrThrow(id);
        policy.assertCanManage(actor);

        String temporary = passwords.generate();
        user.setPasswordHash(encoder.encode(temporary));
        user.setMustChangePassword(true);
        user = users.save(user);

        sessions.invalidate(user.getUsername());
        audit.log("USER_PASSWORD_RESET", "USER", user.getId(),
                "New password issued for " + user.getUsername());
        return new UserCreatedResponse(UserResponse.from(user, knownRoles()), temporary);
    }

    public UserResponse disable(Actor actor, Long id) {
        var user = getOrThrow(id);
        policy.assertCanChangeStatus(actor, user, UserStatus.DISABLED);
        if (user.getStatus() != UserStatus.DISABLED) {
            user.setStatus(UserStatus.DISABLED);
            users.save(user);
            audit.log("USER_DISABLED", "USER", user.getId(), "Account disabled: " + user.getUsername());
        }
        sessions.invalidate(user.getUsername());
        return UserResponse.from(user, knownRoles());
    }

    public UserResponse enable(Actor actor, Long id) {
        var user = getOrThrow(id);
        policy.assertCanChangeStatus(actor, user, UserStatus.ACTIVE);
        if (user.getStatus() == UserStatus.DISABLED) {
            user.setStatus(user.isMustChangePassword() ? UserStatus.INVITED : UserStatus.ACTIVE);
            users.save(user);
            audit.log("USER_ENABLED", "USER", user.getId(), "Account re-enabled: " + user.getUsername());
        }
        return UserResponse.from(user, knownRoles());
    }

    private Set<String> knownRoles() {
        var known = new HashSet<>(policy.assignableRoles());
        known.add(ModuleService.SUPERADMIN);
        return known;
    }

    private static List<RoleGrant> parseRoles(List<String> roles) {
        try {
            return roles.stream().map(RoleGrant::valueOf).toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role grant: " + e.getMessage());
        }
    }

    private static Set<LoginMethod> parseMethods(List<String> methods) {
        try {
            return methods.stream().map(m -> LoginMethod.valueOf(m.trim().toUpperCase())).collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown login method in " + methods);
        }
    }

    /** An email is unique among accounts and must not collide with a configured superadmin, which shadows database accounts. */
    private void assertEmailFree(String email, AppUser self) {
        var clean = email.trim();
        if (superAdmins.find(clean).isPresent()) {
            throw new ConflictException("The address is used by a configured system account");
        }
        users.findByEmailIgnoreCase(clean).filter(u -> self == null || !u.getId().equals(self.getId()))
                .ifPresent(u -> { throw new ConflictException("The email address is already used by another account"); });
        users.findByUsername(clean).filter(u -> self == null || !u.getId().equals(self.getId()))
                .ifPresent(u -> { throw new ConflictException("The username is already used by another account"); });
    }

    private void applyPerson(AppUser user, Long personId) {
        boolean isMember = user.hasRole("member");
        if (personId == null) {
            if (isMember) throw new IllegalArgumentException("An account with the member role must be linked to a person");
            user.setPerson(null);
            return;
        }
        var person = persons.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person " + personId + " was not found"));
        users.findAll().stream()
                .filter(u -> u.getPerson() != null && u.getPerson().getId().equals(personId))
                .filter(u -> user.getId() == null || !u.getId().equals(user.getId()))
                .findAny()
                .ifPresent(u -> { throw new ConflictException("The person already has an account"); });
        user.setPerson(person);
    }

    private static String rolesText(AppUser u) {
        return u.getRoles().stream().map(RoleGrant::toString).sorted().collect(Collectors.joining(", ", "[", "]"));
    }

    private AppUser getOrThrow(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Account " + id + " was not found"));
    }
}
