package se.portplanner.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.portplanner.model.AppUser;
import se.portplanner.model.LoginMethod;
import se.portplanner.model.UserStatus;
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
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var configured = superAdmins.find(username);
        if (configured.isPresent()) {
            var sa = configured.get();
            if (!sa.enabled()) throw notFound(username);
            // Övergång: HARBOUR_MASTER-authority behålls tills SecurityConfig och SPA:erna använder roll-listor.
            return new User(sa.username(), sa.passwordHash(), List.of(
                    new SimpleGrantedAuthority("ROLE_superadmin:rw"),
                    new SimpleGrantedAuthority(LegacyAuthorities.SUPERADMIN)));
        }
        AppUser user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmailIgnoreCase(username))
                .orElseThrow(() -> notFound(username));
        // Samma neutrala fel som fel lösenord om kontot saknar metoden PASSWORD (LOG-12).
        if (!user.getLoginMethods().contains(LoginMethod.PASSWORD)) throw notFound(username);
        boolean enabled = user.getStatus() != UserStatus.DISABLED;
        return new User(user.getUsername(), user.getPasswordHash(), enabled, true, true, true,
                LegacyAuthorities.of(user));
    }

    private static UsernameNotFoundException notFound(String username) {
        return new UsernameNotFoundException("User not found: " + username);
    }
}
