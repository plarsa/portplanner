package se.portplanner.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import se.portplanner.model.Role;
import se.portplanner.repository.AppUserRepository;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final SuperAdminRegistry superAdmins;

    public UserDetailsServiceImpl(AppUserRepository userRepository, SuperAdminRegistry superAdmins) {
        this.userRepository = userRepository;
        this.superAdmins = superAdmins;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var configured = superAdmins.find(username);
        if (configured.isPresent()) {
            var sa = configured.get();
            if (!sa.enabled()) throw new UsernameNotFoundException("User not found: " + username);
            // Övergång: HARBOUR_MASTER-authority behålls tills SecurityConfig och SPA:erna använder roll-listor.
            return new User(sa.username(), sa.passwordHash(), List.of(
                    new SimpleGrantedAuthority("ROLE_superadmin:rw"),
                    new SimpleGrantedAuthority("ROLE_" + Role.HARBOUR_MASTER.name())));
        }
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new User(user.getUsername(), user.getPasswordHash(), authoritiesFor(user.getRole()));
    }

    /**
     * Övergång tills konton har egna rolltilldelningar (fas 3): den enda rollen översätts till en tilldelning,
     * och den gamla authorityn behålls för SecurityConfig och SPA:erna.
     */
    static List<GrantedAuthority> authoritiesFor(Role role) {
        var grant = role == Role.MEMBER ? "member:rw" : "admin:rw";
        return List.of(new SimpleGrantedAuthority("ROLE_" + grant), new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
