package com.team.corporate.backend.services;

import com.team.corporate.backend.config.AuthTokenProperties;
import com.team.corporate.backend.dto.ChangePasswordRequest;
import com.team.corporate.backend.dto.InitialPasswordRequest;
import com.team.corporate.backend.repositories.AuthSessionRepository;
import com.team.corporate.backend.repositories.RefreshTokenRepository;
import com.team.corporate.backend.repositories.adapters.UserJpaRepository;
import com.team.corporate.backend.security.InvalidRefreshTokenException;
import com.team.corporate.backend.security.JwtTokenService;
import com.team.corporate.backend.security.RefreshTokenCodec;
import com.team.corporate.backend.security.SessionValidator;
import com.team.corporate.backend.security.SecurityAudit;
import com.team.corporate.backend.services.responses.LoginResponse;
import com.team.corporate.backend.services.responses.PasswordChangeTokenResponse;
import com.team.corporate.backend.services.responses.TokenPairResponse;
import com.team.corporate.backend.validation.PasswordPolicy;
import com.team.corporate.backend.validation.InvalidPasswordException;
import com.team.corporate.core.entities.AuthSession;
import com.team.corporate.core.entities.RefreshToken;
import com.team.corporate.core.entities.SessionType;
import com.team.corporate.core.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final CredentialAuthenticationService credentials;
    private final UserJpaRepository users;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository tokens;
    private final JwtTokenService jwt;
    private final JwtDecoder decoder;
    private final RefreshTokenCodec refresh;
    private final SessionValidator validator;
    private final AuthTokenProperties properties;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwords;
    private final EntityManager entityManager;
    private final Clock clock;

    public AuthService(CredentialAuthenticationService credentials, UserJpaRepository users,
                       AuthSessionRepository sessions, RefreshTokenRepository tokens, JwtTokenService jwt,
                       JwtDecoder decoder, RefreshTokenCodec refresh, SessionValidator validator,
                       AuthTokenProperties properties, PasswordEncoder encoder, PasswordPolicy passwords,
                       EntityManager entityManager, Clock clock) {
        this.credentials = credentials;
        this.users = users;
        this.sessions = sessions;
        this.tokens = tokens;
        this.jwt = jwt;
        this.decoder = decoder;
        this.refresh = refresh;
        this.validator = validator;
        this.properties = properties;
        this.encoder = encoder;
        this.passwords = passwords;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public LoginResponse login(String username, String password) {
        var checked = credentials.authenticate(username, password);
        var user = lockUser(checked.id());
        // A password reset/block could commit between the credential check and obtaining this lock.
        if (!user.isEnabled() || user.getPasswordHash() == null || !encoder.matches(password, user.getPasswordHash())) {
            throw invalid();
        }
        if (user.isMustChangePassword()) {
            Instant expires = now().plus(properties.passwordChangeTtl());
            if (user.getTemporaryPasswordExpiresAt() == null || !user.getTemporaryPasswordExpiresAt().isAfter(now())) {
                throw invalid();
            }
            if (expires.isAfter(user.getTemporaryPasswordExpiresAt())) {
                expires = user.getTemporaryPasswordExpiresAt();
            }
            var session = sessions.saveAndFlush(new AuthSession(user, now(), expires, SessionType.PASSWORD_CHANGE));
            var issued = jwt.issue(session);
            SecurityAudit.success("LOGIN_LIMITED", user.getId(), user.getId());
            return new LoginResponse(LoginResponse.Status.PASSWORD_CHANGE_REQUIRED, null,
                    new PasswordChangeTokenResponse(issued.value(), issued.expiresAt()));
        }
        var pair = createFullSession(user);
        SecurityAudit.success("LOGIN", user.getId(), user.getId());
        return new LoginResponse(LoginResponse.Status.AUTHENTICATED, pair, null);
    }

    @Transactional
    public TokenPairResponse initialPassword(String limitedToken, InitialPasswordRequest request) {
        Jwt claims = decode(limitedToken, "PASSWORD_CHANGE");
        var user = lockUser(UUID.fromString(claims.getSubject()));
        var session = lockSession(UUID.fromString(claims.getClaimAsString("sid")));
        requireActive(session, user, SessionType.PASSWORD_CHANGE);
        passwords.validateNewPassword(request.newPassword());
        if (encoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Новый пароль должен отличаться от временного.");
        }
        user.setPermanentPassword(encoder.encode(request.newPassword()));
        revokeAll(user.getId());
        SecurityAudit.success("PASSWORD_INITIAL", user.getId(), user.getId());
        return createFullSession(user);
    }

    @Transactional
    public void changePassword(String accessToken, ChangePasswordRequest request) {
        Jwt claims = decode(accessToken, "ACCESS");
        var user = lockUser(UUID.fromString(claims.getSubject()));
        var session = lockSession(UUID.fromString(claims.getClaimAsString("sid")));
        requireActive(session, user, SessionType.FULL);
        if (!PasswordPolicy.isBcryptCompatible(request.currentPassword())
                || !encoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw invalid();
        }
        passwords.validateNewPassword(request.newPassword());
        if (encoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Новый пароль должен отличаться от текущего.");
        }
        user.setPermanentPassword(encoder.encode(request.newPassword()));
        revokeAll(user.getId());
        SecurityAudit.success("PASSWORD_CHANGE", user.getId(), user.getId());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPairResponse refresh(String rawToken) {
        String hash = refresh.hash(rawToken);
        var reference = tokens.findReferenceByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);
        var user = lockUser(reference.userId());
        var session = lockSession(reference.sessionId());
        var token = tokens.findByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);
        if (!session.getUser().getId().equals(user.getId()) || session.getSessionType() != SessionType.FULL
                || !validator.isActive(session, user) || !token.getExpiresAt().isAfter(now())) {
            throw new InvalidRefreshTokenException();
        }
        if (token.getUsedAt() != null) {
            session.setRevokedAt(now());
            SecurityAudit.success("REFRESH_REPLAY_REVOKE", user.getId(), user.getId());
            // This rejection must commit the revocation rather than roll it back with the exception.
            throw new InvalidRefreshTokenException();
        }
        token.setUsedAt(now());
        SecurityAudit.success("REFRESH", user.getId(), user.getId());
        return issuePair(session);
    }

    @Transactional
    public void logout(String rawToken) {
        String hash = refresh.hash(rawToken);
        var reference = tokens.findReferenceByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);
        var user = lockUser(reference.userId());
        var session = lockSession(reference.sessionId());
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(now());
        }
        SecurityAudit.success("LOGOUT", user.getId(), user.getId());
    }

    @Transactional
    public void logoutAll(String accessToken) {
        Jwt claims = decode(accessToken, "ACCESS");
        var user = lockUser(UUID.fromString(claims.getSubject()));
        var session = lockSession(UUID.fromString(claims.getClaimAsString("sid")));
        requireActive(session, user, SessionType.FULL);
        revokeAll(user.getId());
        SecurityAudit.success("LOGOUT_ALL", user.getId(), user.getId());
    }

    /** Bounded maintenance operation; history survives rotation and remains until absolute expiry plus retention. */
    @Transactional
    public int purgeExpiredHistory() {
        Instant cutoff = now().minus(properties.historyRetention());
        int deleted = 0;
        for (var reference : sessions.findExpiredReferences(cutoff, org.springframework.data.domain.PageRequest.of(0, 100))) {
            lockUser(reference.userId());
            var found = sessions.findByIdForUpdate(reference.sessionId());
            if (found.isEmpty()) {
                continue;
            }
            var session = found.get();
            entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE);
            if (session.getExpiresAt().isAfter(cutoff)
                    || tokens.existsBySessionIdAndExpiresAtAfter(session.getId(), cutoff)) {
                continue;
            }
            tokens.deleteAllBySessionId(session.getId());
            tokens.flush();
            sessions.delete(session);
            deleted++;
        }
        return deleted;
    }

    private TokenPairResponse createFullSession(User user) {
        var session = sessions.saveAndFlush(new AuthSession(user, now(),
                now().plus(properties.sessionTtl()), SessionType.FULL));
        return issuePair(session);
    }

    private TokenPairResponse issuePair(AuthSession session) {
        String raw = refresh.generate();
        tokens.save(new RefreshToken(session, refresh.hash(raw), now(), session.getExpiresAt()));
        var access = jwt.issue(session);
        return new TokenPairResponse(access.value(), access.expiresAt(), raw, session.getExpiresAt());
    }

    private User lockUser(UUID id) {
        var user = users.findByIdForUpdate(id).orElseThrow(AuthService::invalid);
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    private AuthSession lockSession(UUID id) {
        var session = sessions.findByIdForUpdate(id).orElseThrow(AuthService::invalid);
        entityManager.refresh(session, LockModeType.PESSIMISTIC_WRITE);
        return session;
    }

    private void requireActive(AuthSession session, User user, SessionType type) {
        if (session.getSessionType() != type || !session.getUser().getId().equals(user.getId()) || !validator.isActive(session, user)) {
            throw invalid();
        }
    }

    private Jwt decode(String token, String purpose) {
        try {
            var claims = decoder.decode(token);
            if (!purpose.equals(claims.getClaimAsString("purpose"))) {
                throw invalid();
            }
            return claims;
        } catch (JwtException | IllegalArgumentException error) {
            throw invalid();
        }
    }

    private void revokeAll(UUID userId) {
        for (var session : sessions.findAllByUserId(userId)) {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(now());
            }
        }
    }

    private Instant now() {
        return clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    private static BadCredentialsException invalid() {
        return new BadCredentialsException("Недействительные полномочия или сессия.");
    }
}
