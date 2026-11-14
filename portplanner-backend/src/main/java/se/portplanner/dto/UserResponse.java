package se.portplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import se.portplanner.model.AppUser;
import se.portplanner.module.RoleGrant;

import java.time.LocalDateTime;
import java.util.List;

/** An account without password details. {@code readOnly} marks a configured superadmin, which has no database row. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        Long id,
        String username,
        String email,
        List<String> roles,
        List<String> unknownRoles,
        List<String> loginMethods,
        String status,
        boolean mustChangePassword,
        Long personId,
        String personName,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        boolean readOnly
) {
    public static UserResponse from(AppUser u, java.util.Set<String> knownRoles) {
        var roles = u.getRoles().stream().map(RoleGrant::toString).sorted().toList();
        var unknown = u.getRoles().stream().map(RoleGrant::role)
                .filter(r -> !knownRoles.contains(r)).sorted().toList();
        var person = u.getPerson();
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), roles, unknown,
                u.getLoginMethods().stream().map(Enum::name).sorted().toList(),
                u.getStatus().name(), u.isMustChangePassword(),
                person == null ? null : person.getId(),
                person == null ? null : person.getFirstName() + " " + person.getLastName(),
                u.getLastLoginAt(), u.getCreatedAt(), false);
    }

    public static UserResponse configuredSuperadmin(String username, boolean enabled) {
        return new UserResponse(null, username, null, List.of("superadmin:rw"), List.of(), List.of("PASSWORD"),
                enabled ? "ACTIVE" : "DISABLED", false, null, null, null, null, true);
    }
}
