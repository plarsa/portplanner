package se.portplanner.security;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Component;

/** Avslutar alla sessioner för ett konto, t.ex. när det inaktiveras eller får ändrade roller. */
@Component
public class SessionInvalidator {

    private final FindByIndexNameSessionRepository<?> sessions;

    public SessionInvalidator(FindByIndexNameSessionRepository<?> sessions) {
        this.sessions = sessions;
    }

    public int invalidate(String username) {
        if (username == null) return 0;
        var found = sessions.findByPrincipalName(username);
        found.keySet().forEach(sessions::deleteById);
        return found.size();
    }
}
