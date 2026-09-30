package se.portplanner.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Slumpar engångslösenord med tecken som inte går att förväxla. */
@Component
public class PasswordGenerator {

    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        var sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
