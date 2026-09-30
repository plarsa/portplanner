package se.portplanner.module;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * En roll med nivå, skriven {@code roll:nivå} (t.ex. {@code admin:rw}). Används både för kontots
 * tilldelningar och för posterna i en moduls {@code access}-lista.
 */
@Embeddable
public record RoleGrant(
        @Column(name = "role_name") String role,
        @Enumerated(EnumType.STRING) @Column(name = "access_level") Level level) {

    public static final String AUTHORITY_PREFIX = "ROLE_";
    private static final Pattern ROLE_NAME = Pattern.compile("[a-z][a-z0-9_]*");

    public enum Level {
        R, RW;

        public AccessLevel toAccessLevel() {
            return this == RW ? AccessLevel.WRITE : AccessLevel.READ;
        }

        public static Level min(Level a, Level b) {
            return a.compareTo(b) <= 0 ? a : b;
        }

        public static Level max(Level a, Level b) {
            return a.compareTo(b) >= 0 ? a : b;
        }
    }

    public RoleGrant {
        if (role == null || !ROLE_NAME.matcher(role).matches()) {
            throw new IllegalArgumentException("Ogiltigt rollnamn: '" + role + "' (tillåtet: [a-z][a-z0-9_]*)");
        }
        if (level == null) throw new IllegalArgumentException("Nivå saknas för roll " + role);
    }

    /** Läser {@code roll:r} eller {@code roll:rw}. Används även av Spring vid bindning av konfiguration. */
    public static RoleGrant valueOf(String text) {
        if (text == null) throw new IllegalArgumentException("Tom rolltilldelning");
        int i = text.lastIndexOf(':');
        if (i <= 0 || i == text.length() - 1) {
            throw new IllegalArgumentException("Ogiltig post '" + text + "', förväntade roll:nivå (t.ex. admin:rw)");
        }
        var level = switch (text.substring(i + 1).trim().toLowerCase()) {
            case "r" -> Level.R;
            case "rw" -> Level.RW;
            default -> throw new IllegalArgumentException(
                    "Ogiltig nivå i '" + text + "', tillåtna nivåer är r och rw");
        };
        return new RoleGrant(text.substring(0, i).trim(), level);
    }

    /** Tolkar en Spring Security-authority {@code ROLE_roll:nivå}; andra authorities ger tomt. */
    public static Optional<RoleGrant> fromAuthority(String authority) {
        if (authority == null || !authority.startsWith(AUTHORITY_PREFIX)) return Optional.empty();
        try {
            return Optional.of(valueOf(authority.substring(AUTHORITY_PREFIX.length())));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String toAuthority() {
        return AUTHORITY_PREFIX + this;
    }

    @Override
    public String toString() {
        return role + ":" + level.name().toLowerCase();
    }
}
