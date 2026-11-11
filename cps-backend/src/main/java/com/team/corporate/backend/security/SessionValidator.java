package com.team.corporate.backend.security;

import com.team.corporate.backend.repositories.AuthSessionRepository;
import com.team.corporate.core.entities.AuthSession;
import com.team.corporate.core.entities.SessionType;
import com.team.corporate.core.entities.User;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Component
public class SessionValidator {
    private final AuthSessionRepository sessions;
    private final Clock clock;

    public SessionValidator(AuthSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SessionPrincipal validate(Jwt jwt) {
        try {
            var session = sessions.findById(UUID.fromString(jwt.getClaimAsString("sid"))).orElseThrow();
            var user = session.getUser();
            SessionType expected = "ACCESS".equals(jwt.getClaimAsString("purpose"))
                    ? SessionType.FULL : SessionType.PASSWORD_CHANGE;
            if (!user.getId().equals(UUID.fromString(jwt.getSubject())) || session.getSessionType() != expected
                    || !isActive(session, user)) {
                throw new IllegalArgumentException();
            }
            return new SessionPrincipal(user.getId(), session.getId(), user.getUsername(), user.getUserRole(), expected);
        } catch (java.util.NoSuchElementException | IllegalArgumentException invalid) {
            throw new OAuth2AuthenticationException("invalid_token");
        }
    }

    public boolean isActive(AuthSession session, User user) {
        if (!user.isEnabled() || session.getRevokedAt() != null || !session.getExpiresAt().isAfter(clock.instant())) {
            return false;
        }
        if (session.getSessionType() == SessionType.FULL) {
            return !user.isMustChangePassword();
        }
        return user.isMustChangePassword() && user.getTemporaryPasswordExpiresAt() != null
                && user.getTemporaryPasswordExpiresAt().isAfter(clock.instant());
    }
}
