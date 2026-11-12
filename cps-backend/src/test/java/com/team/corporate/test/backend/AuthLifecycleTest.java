package com.team.corporate.test.backend;

import com.team.corporate.backend.dto.ChangePasswordRequest;
import com.team.corporate.backend.dto.InitialPasswordRequest;
import com.team.corporate.backend.security.InvalidRefreshTokenException;
import com.team.corporate.backend.services.responses.LoginResponse;
import com.team.corporate.core.entities.SessionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthLifecycleTest extends AuthLifecycleTestBase {
    @Autowired private PasswordEncoder encoder;

    @Test
    void creationDoesNotLoginAndTemporaryLoginOnlyCreatesLimitedSession() {
        var created = management.createUser("limited_user", com.team.corporate.core.entities.UserRole.USER);
        assertEquals(0, count("auth_sessions"));
        jdbc.update("UPDATE users SET temporary_password_expires_at = ? WHERE id = ?",
                java.sql.Timestamp.from(clock.now.plusSeconds(60)), created.id());
        var login = auth.login("LIMITED_USER", created.temporaryPassword());
        assertEquals(LoginResponse.Status.PASSWORD_CHANGE_REQUIRED, login.status());
        assertNull(login.tokens());
        assertNotNull(login.passwordChange());
        assertEquals(clock.now.plusSeconds(60), login.passwordChange().expiresAt());
        assertEquals(0, count("refresh_tokens"));
        assertEquals(SessionType.PASSWORD_CHANGE, validator.validate(decoder.decode(login.passwordChange().token())).type());
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(login.passwordChange().token()));
        assertFalse(login.toString().contains(login.passwordChange().token()));
    }

    @Test
    void initialChangeIsOneTimeAndRevokesEveryOtherLimitedSession() {
        var created = management.createUser("initial_user", com.team.corporate.core.entities.UserRole.USER);
        var first = auth.login(created.username(), created.temporaryPassword());
        var second = auth.login(created.username(), created.temporaryPassword());
        var old = decoder.decode(second.passwordChange().token());
        var full = auth.initialPassword(first.passwordChange().token(), new InitialPasswordRequest(PASSWORD));
        var user = users.findById(created.id()).orElseThrow();
        assertFalse(user.isMustChangePassword());
        assertNull(user.getTemporaryPasswordExpiresAt());
        assertTrue(encoder.matches(PASSWORD, user.getPasswordHash()));
        assertRejected(old);
        assertThrows(BadCredentialsException.class,
                () -> auth.initialPassword(first.passwordChange().token(), new InitialPasswordRequest(NEXT_PASSWORD)));
        assertEquals(SessionType.FULL, validator.validate(decoder.decode(full.accessToken())).type());
        assertEquals(LoginResponse.Status.AUTHENTICATED, auth.login(created.username(), PASSWORD).status());
        assertThrows(BadCredentialsException.class, () -> auth.login(created.username(), created.temporaryPassword()));
    }

    @Test
    void invalidInitialPasswordsLeaveChallengeUsable() {
        var created = management.createUser("invalid_initial", com.team.corporate.core.entities.UserRole.USER);
        var login = auth.login(created.username(), created.temporaryPassword());
        assertThrows(IllegalArgumentException.class,
                () -> auth.initialPassword(login.passwordChange().token(), new InitialPasswordRequest("short")));
        assertThrows(IllegalArgumentException.class,
                () -> auth.initialPassword(login.passwordChange().token(), new InitialPasswordRequest(created.temporaryPassword())));
        assertTrue(users.findById(created.id()).orElseThrow().isMustChangePassword());
        assertDoesNotThrow(() -> auth.initialPassword(login.passwordChange().token(), new InitialPasswordRequest(PASSWORD)));
    }

    @Test
    void rotationKeepsHistoryAndReplayCommitsRevocationDespiteThrowing() {
        var first = activate("rotation_user");
        var oldAccess = decoder.decode(first.accessToken());
        var next = auth.refresh(first.refreshToken());
        assertFalse(first.refreshToken().equals(next.refreshToken()));
        assertFalse(first.accessToken().equals(next.accessToken()));
        assertEquals(first.sessionExpiresAt(), next.sessionExpiresAt());
        assertNotNull(tokens.findByTokenHash(refreshCodec.hash(first.refreshToken())).orElseThrow().getUsedAt());
        assertEquals(2, tokens.findAllBySessionId(sessionId(next.accessToken())).size());
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(first.refreshToken()));
        assertRejected(oldAccess);
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(next.refreshToken()));
    }

    @Test
    void logoutIsIdempotentWorksWithoutAccessAndRevokesOnlyItsSession() {
        var first = activate("logout_user");
        var other = auth.login("logout_user", PASSWORD).tokens();
        var access = decoder.decode(first.accessToken());
        auth.logout(first.refreshToken());
        auth.logout(first.refreshToken());
        assertRejected(access);
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(first.refreshToken()));
        assertDoesNotThrow(() -> auth.refresh(other.refreshToken()));
        assertThrows(InvalidRefreshTokenException.class, () -> auth.logout(refreshCodec.generate()));
    }

    @Test
    void accessExpiryAllowsRefreshButAbsoluteSessionExpiryDoesNot() {
        var first = activate("expiry_user");
        clock.now = first.accessExpiresAt();
        assertThrows(JwtException.class, () -> decoder.decode(first.accessToken()));
        var next = auth.refresh(first.refreshToken());
        clock.now = first.sessionExpiresAt().minusSeconds(10);
        var last = auth.refresh(next.refreshToken());
        assertEquals(first.sessionExpiresAt(), last.accessExpiresAt());
        clock.now = first.sessionExpiresAt();
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(last.refreshToken()));
        assertThrows(JwtException.class, () -> decoder.decode(last.accessToken()));
    }

    @Test
    void temporaryJwtExpiresAtItsOwnDeadlineAndCannotMakeFullSession() {
        var limited = limited("challenge_expiry").passwordChange();
        clock.now = limited.expiresAt();
        assertThrows(BadCredentialsException.class,
                () -> auth.initialPassword(limited.token(), new InitialPasswordRequest(PASSWORD)));
        assertEquals(0, count("refresh_tokens"));
    }

    @Test
    void validatorUsesCurrentRoleStatusOwnershipAndSessionType() {
        var pair = activate("claims_user");
        var claims = decoder.decode(pair.accessToken());
        var id = UUID.fromString(claims.getSubject());
        jdbc.update("UPDATE users SET user_role = 'MANAGER' WHERE id = ?", id);
        assertEquals(com.team.corporate.core.entities.UserRole.MANAGER, validator.validate(claims).role());
        jdbc.update("UPDATE users SET enabled = FALSE WHERE id = ?", id);
        assertRejected(claims);
        jdbc.update("UPDATE users SET enabled = TRUE WHERE id = ?", id);
        var forged = new HashMap<String, Object>(claims.getClaims());
        forged.put("sub", UUID.randomUUID().toString());
        assertRejected(decoder.decode(sign(forged)));
        forged = new HashMap<>(claims.getClaims());
        forged.put("purpose", "PASSWORD_CHANGE");
        assertRejected(decoder.decode(sign(forged)));
    }

    @Test
    void ordinaryPasswordChangeRequiresCurrentPasswordAndInvalidatesAllOldSessions() {
        var pair = activate("change_user");
        var other = auth.login("change_user", PASSWORD).tokens();
        var access = decoder.decode(other.accessToken());
        assertThrows(BadCredentialsException.class,
                () -> auth.changePassword(pair.accessToken(), new ChangePasswordRequest("wrong", NEXT_PASSWORD)));
        assertThrows(IllegalArgumentException.class,
                () -> auth.changePassword(pair.accessToken(), new ChangePasswordRequest(PASSWORD, PASSWORD)));
        auth.changePassword(pair.accessToken(), new ChangePasswordRequest(PASSWORD, NEXT_PASSWORD));
        assertRejected(access);
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(other.refreshToken()));
        assertThrows(BadCredentialsException.class, () -> auth.login("change_user", PASSWORD));
        assertEquals(LoginResponse.Status.AUTHENTICATED, auth.login("change_user", NEXT_PASSWORD).status());
    }

    @Test
    void logoutAllCannotUseAnotherUsersSessionAndOnlyRevokesCurrentUsersSessions() {
        var first = activate("all_user");
        var second = auth.login("all_user", PASSWORD).tokens();
        var another = activate("another_user");
        var claims = new HashMap<String, Object>(decoder.decode(first.accessToken()).getClaims());
        claims.put("sid", sessionId(another.accessToken()).toString());
        assertThrows(BadCredentialsException.class, () -> auth.logoutAll(sign(claims)));
        auth.logoutAll(first.accessToken());
        assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(second.refreshToken()));
        assertDoesNotThrow(() -> auth.refresh(another.refreshToken()));
    }

    @Test
    void signingFailureRollsBackRotationAndInitialChange() {
        var pair = activate("rollback_rotation");
        signer.fail = true;
        assertThrows(JwtEncodingException.class, () -> auth.refresh(pair.refreshToken()));
        signer.fail = false;
        assertNull(tokens.findByTokenHash(refreshCodec.hash(pair.refreshToken())).orElseThrow().getUsedAt());
        assertDoesNotThrow(() -> auth.refresh(pair.refreshToken()));
        var limited = limited("rollback_initial").passwordChange();
        signer.fail = true;
        assertThrows(JwtEncodingException.class,
                () -> auth.initialPassword(limited.token(), new InitialPasswordRequest(PASSWORD)));
        signer.fail = false;
        assertTrue(users.findByUsername("rollback_initial").orElseThrow().isMustChangePassword());
        assertDoesNotThrow(() -> auth.initialPassword(limited.token(), new InitialPasswordRequest(PASSWORD)));
    }

    @Test
    void cleanupRetainsReplayHistoryUntilExpiryAndRetention() {
        var first = activate("cleanup_old");
        auth.refresh(first.refreshToken());
        auth.logout(first.refreshToken());
        assertEquals(0, auth.purgeExpiredHistory());
        clock.now = clock.now.plus(Duration.ofDays(2));
        var newer = activate("cleanup_new");
        clock.now = first.sessionExpiresAt().plus(Duration.ofDays(1));
        assertTrue(auth.purgeExpiredHistory() > 0);
        assertTrue(tokens.findByTokenHash(refreshCodec.hash(first.refreshToken())).isEmpty());
        assertDoesNotThrow(() -> auth.refresh(newer.refreshToken()));
    }
}
