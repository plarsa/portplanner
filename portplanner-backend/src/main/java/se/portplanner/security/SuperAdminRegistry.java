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
 *
 * <p>When no enabled superadmin is configured the service still starts: it falls back to a
 * {@value #GENERATED_USERNAME} account whose password is generated for that run and logged once.
 */
@Component
public class SuperAdminRegistry {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminRegistry.class);

    /** The account used when the configuration names no enabled superadmin. */
    static final String GENERATED_USERNAME = "superadmin";

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
            generateAccount(encoder);
        }
        log.info("Configured superadmin accounts: {}", accounts.values().stream()
                .map(a -> a.username() + (a.enabled() ? "" : " (disabled)")).toList());
    }

    /**
     * Keeps the service startable without configuration. The password is new on every start, so it is
     * useless once the log line scrolls past; a real deployment configures its own account instead.
     */
    private void generateAccount(BCryptPasswordEncoder encoder) {
        var password = new PasswordGenerator().generate();
        accounts.put(key(GENERATED_USERNAME), new Account(GENERATED_USERNAME, encoder.encode(password), true));
        log.warn("""
                No enabled superadmin is configured. Using a generated password for this run only:
                  username: {}
                  password: {}
                Set portplanner.iam.superadmins (for example through the SUPERADMIN_USERNAME and \
                SUPERADMIN_PASSWORD environment variables) to keep an account across restarts.""",
                GENERATED_USERNAME, password);
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
