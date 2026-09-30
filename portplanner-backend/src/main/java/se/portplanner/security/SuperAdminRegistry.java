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
 * Konfigurerade superadmin-konton (ROL-11). De finns bara i applikationskonfigurationen, aldrig i databasen.
 * Lösenordet anges som bcrypt-hash (med eller utan {@code {bcrypt}}-prefix). Klartext accepteras men ger varning.
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
                        "portplanner.iam.superadmins: varje post kräver både username och password");
            }
            var key = key(sa.getUsername());
            if (accounts.containsKey(key)) {
                throw new IllegalStateException("portplanner.iam.superadmins: användarnamnet '" + sa.getUsername() + "' förekommer flera gånger");
            }
            accounts.put(key, new Account(sa.getUsername(), toHash(sa.getPassword(), encoder), sa.isEnabled()));
            anyEnabled |= sa.isEnabled();
        }
        if (!anyEnabled) {
            throw new IllegalStateException(
                    "Ingen aktiverad superadmin är konfigurerad. Ange portplanner.iam.superadmins "
                            + "(t.ex. via miljövariablerna SUPERADMIN_USERNAME och SUPERADMIN_PASSWORD) med enabled=true.");
        }
        log.info("Konfigurerade superadmin-konton: {}", accounts.values().stream()
                .map(a -> a.username() + (a.enabled() ? "" : " (avstängd)")).toList());
    }

    private String toHash(String password, BCryptPasswordEncoder encoder) {
        if (password.startsWith("{bcrypt}")) return password.substring("{bcrypt}".length());
        if (password.startsWith("$2")) return password;
        log.warn("En konfigurerad superadmin har lösenord i klartext. Använd en bcrypt-hash (portplanner.iam.superadmins[].password).");
        return encoder.encode(password);
    }

    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /** Träff även för avstängda konton, så att inloggning inte faller tillbaka på databaskonto med samma namn. */
    public Optional<Account> find(String username) {
        return username == null ? Optional.empty() : Optional.ofNullable(accounts.get(key(username)));
    }

    public Collection<Account> all() {
        return java.util.List.copyOf(accounts.values());
    }
}
