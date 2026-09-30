package se.portplanner.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import se.portplanner.module.RoleGrant;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Inloggningskonto i databasen. {@code username} är alltid e-postadressen och kan inte sättas separat.
 * Konfigurerade superadmin finns inte här (se SuperAdminRegistry).
 */
@Entity
@Table(name = "app_users")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Följer alltid e-postadressen. Null-email är bara möjligt för konton som skapats före IAM-ändringen. */
    @Setter(AccessLevel.NONE)
    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Setter(AccessLevel.NONE)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<RoleGrant> roles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_login_methods", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "method")
    @Enumerated(EnumType.STRING)
    private Set<LoginMethod> loginMethods = new HashSet<>(Set.of(LoginMethod.PASSWORD));

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    public void setEmail(String email) {
        this.email = email;
        if (email != null) this.username = email;
    }

    /** Ersätter rolltilldelningarna. Samma roll högst en gång; den högsta nivån gäller (ROL-1). */
    public void setRoles(Collection<RoleGrant> grants) {
        var highest = new HashMap<String, RoleGrant.Level>();
        grants.forEach(g -> highest.merge(g.role(), g.level(), RoleGrant.Level::max));
        roles.clear();
        highest.forEach((role, level) -> roles.add(new RoleGrant(role, level)));
    }

    public boolean hasRole(String role) {
        return roles.stream().anyMatch(g -> g.role().equals(role));
    }
}
