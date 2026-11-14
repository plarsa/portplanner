package se.portplanner.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** The settings under {@code portplanner.iam}. */
@ConfigurationProperties("portplanner.iam")
public class IamProperties {

    /** Konfigurerade superadmin-konton. De lagras aldrig i databasen. */
    private List<SuperAdmin> superadmins = new ArrayList<>();

    public List<SuperAdmin> getSuperadmins() { return superadmins; }
    public void setSuperadmins(List<SuperAdmin> superadmins) { this.superadmins = superadmins; }

    public static class SuperAdmin {
        private String username;
        private String password;
        private boolean enabled = true;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        boolean isBlank() {
            return (username == null || username.isBlank()) && (password == null || password.isBlank());
        }
    }
}
