package se.portplanner.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The configured superadmin accounts (ROL-11). They live only in the application configuration, never in
 * the database. The password is a bcrypt hash, with or without the {@code {bcrypt}} prefix. Cleartext is
 * accepted but logs a warning.
 */
@Component
public class SuperAdminRegistry {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminRegistry.class);

    public record Account(String username, String passwordHash, boolean enabled) {}

    private final IamProperties props;
    private final Map<String, Account> accounts = new HashMap<>();

    public SuperAdminRegistry(IamProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        var encoder = new BCryptPasswordEncoder();
        boolean anyEnabled = false;
        for (var sa : props.getSuperadmins()) {
            if (sa.isBlank()) continue;
            if (sa.getUsername() == null || sa.getUsername().isBlank() || sa.getPassword() == null || sa.getPassword().isBlank()) {
                throw new IllegalStateException(
                        "portplanner.iam.superadmins: every entry needs both username and password");
            }
            var key = key(sa.getUsername());
            if (accounts.containsKey(key)) {
                throw new IllegalStateException("portplanner.iam.superadmins: the username '" + sa.getUsername() + "' occurs more than once");
            }
            accounts.put(key, new Account(sa.getUsername(), toHash(sa.getPassword(), encoder), sa.isEnabled()));
            anyEnabled |= sa.isEnabled();
        }
        if (!anyEnabled) {
            throw new IllegalStateException(
                    "No enabled superadmin is configured. Set portplanner.iam.superadmins "
                            + "(for example through the SUPERADMIN_USERNAME and SUPERADMIN_PASSWORD environment variables) with enabled=true.");
        }
        log.info("Configured superadmin accounts: {}", accounts.values().stream()
                .map(a -> a.username() + (a.enabled() ? "" : " (disabled)")).toList());
    }

    private String toHash(String password, BCryptPasswordEncoder encoder) {
        if (password.startsWith("{bcrypt}")) return password.substring("{bcrypt}".length());
        if (password.startsWith("$2")) return password;
        log.warn("A configured superadmin has a cleartext password. Use a bcrypt hash (portplanner.iam.superadmins[].password).");
        return encoder.encode(password);
    }

    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /** Matches disabled accounts too, so that sign-in never falls back to a database account of the same name. */
    public Optional<Account> find(String username) {
        return username == null ? Optional.empty() : Optional.ofNullable(accounts.get(key(username)));
    }

    public Collection<Account> all() {
        return java.util.List.copyOf(accounts.values());
    }
}
