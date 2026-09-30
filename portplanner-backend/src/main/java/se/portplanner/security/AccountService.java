package se.portplanner.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.dto.LoginResponse;
import se.portplanner.model.AppUser;
import se.portplanner.model.LoginMethod;
import se.portplanner.model.UserStatus;
import se.portplanner.module.RoleGrant;
import se.portplanner.repository.AppUserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AccountService {

    private final AppUserRepository users;
    private final SuperAdminRegistry superAdmins;

    public AccountService(AppUserRepository users, SuperAdminRegistry superAdmins) {
        this.users = users;
        this.superAdmins = superAdmins;
    }

    @Transactional
    public void recordLogin(String username) {
        if (superAdmins.find(username).isPresent()) return;
        users.findByUsername(username).ifPresent(u -> u.setLastLoginAt(LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public LoginResponse describe(Authentication auth) {
        var roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .flatMap(a -> RoleGrant.fromAuthority(a).stream())
                .map(RoleGrant::toString)
                .sorted()
                .toList();
        var legacyRole = LegacyAuthorities.roleName(auth);
        if (superAdmins.find(auth.getName()).isPresent()) {
            return new LoginResponse(auth.getName(), legacyRole, null, roles,
                    List.of(LoginMethod.PASSWORD.name()), UserStatus.ACTIVE.name(), null);
        }
        var user = users.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return new LoginResponse(auth.getName(), legacyRole, null, roles, List.of(), null, null);
        }
        return new LoginResponse(user.getUsername(), legacyRole, user.getEmail(), roles,
                user.getLoginMethods().stream().map(Enum::name).sorted(Comparator.naturalOrder()).toList(),
                user.getStatus().name(),
                user.getPerson() == null ? null : user.getPerson().getId());
    }
}
