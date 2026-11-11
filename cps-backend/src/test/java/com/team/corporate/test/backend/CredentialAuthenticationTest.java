package com.team.corporate.test.backend;

import com.team.corporate.backend.repositories.adapters.UserJpaRepository;
import com.team.corporate.backend.security.ProcurementUserDetails;
import com.team.corporate.backend.services.CredentialAuthenticationService;
import com.team.corporate.backend.services.UserManagementService;
import com.team.corporate.core.entities.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(CredentialAuthenticationTest.TestBeans.class)
class CredentialAuthenticationTest extends BackendIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    private static final String PERMANENT_PASSWORD = "  A long test phrase 57!  ";
    @Autowired private CredentialAuthenticationService authentication;
    @Autowired private AuthenticationManager manager;
    @Autowired private UserManagementService management;
    @Autowired private UserJpaRepository users;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CountingEncoder encoder;
    @Autowired private MutableClock clock;

    @BeforeEach
    void resetClockAndCounter() {
        clock.now = NOW;
        encoder.matches.set(0);
    }

    enum Rejection { UNKNOWN_USER, WRONG_PASSWORD, DISABLED, EXPIRED, EXACT_EXPIRY, MISSING_HASH, INVALID_USERNAME }

    @ParameterizedTest
    @EnumSource(Rejection.class)
    void failuresHaveTheSameMessageAndStillPerformBcrypt(Rejection rejection) {
        var created = management.createUser("matrix_user", UserRole.USER);
        String username = created.username();
        String password = created.temporaryPassword();
        switch (rejection) {
            case UNKNOWN_USER -> username = "unknown_user";
            case INVALID_USERNAME -> username = "Иван";
            case WRONG_PASSWORD -> password = "A different test phrase 99!";
            case DISABLED -> jdbc.update("UPDATE users SET enabled = FALSE WHERE id = ?", created.id());
            case MISSING_HASH -> jdbc.update("UPDATE users SET enabled = FALSE, password_hash = NULL WHERE id = ?", created.id());
            case EXPIRED -> clock.now = NOW.plus(Duration.ofHours(24)).plusNanos(1);
            case EXACT_EXPIRY -> clock.now = NOW.plus(Duration.ofHours(24));
        }
        String attemptedUsername = username;
        String attemptedPassword = password;
        var error = assertThrows(BadCredentialsException.class,
                () -> authentication.authenticate(attemptedUsername, attemptedPassword));
        assertEquals(CredentialAuthenticationService.INVALID_CREDENTIALS, error.getMessage());
        assertEquals(1, encoder.matches.get());
        assertEquals(0, count("auth_sessions"));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void validTemporaryPasswordKeepsTheChangeRequirementAndCreatesNoSession(UserRole role) {
        var created = management.createUser("  FIRST.User  ", role);
        var result = authentication.authenticate(" FIRST.USER ", created.temporaryPassword());
        assertEquals(created.id(), result.id());
        assertEquals("first.user", result.username());
        assertEquals(role, result.role());
        assertTrue(result.mustChangePassword());
        assertEquals(NOW.plus(Duration.ofHours(24)), result.temporaryPasswordExpiresAt());
        assertFalse(result.toString().contains(created.temporaryPassword()));
        assertEquals(0, count("auth_sessions"));
        assertEquals(0, count("refresh_tokens"));
    }

    @Test
    void permanentPasswordIsNotTrimmedAndSuccessfulPrincipalErasesHash() {
        var created = management.createUser("permanent_user", UserRole.MANAGER);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var user = users.findById(created.id()).orElseThrow();
            user.setPermanentPassword(encoder.encode(PERMANENT_PASSWORD));
            users.saveAndFlush(user);
        });
        var result = authentication.authenticate("PERMANENT_USER", PERMANENT_PASSWORD);
        assertFalse(result.mustChangePassword());
        assertNull(result.temporaryPasswordExpiresAt());
        assertThrows(BadCredentialsException.class,
                () -> authentication.authenticate("permanent_user", PERMANENT_PASSWORD.strip()));
        assertThrows(BadCredentialsException.class,
                () -> authentication.authenticate("permanent_user", PERMANENT_PASSWORD.toLowerCase(java.util.Locale.ROOT)));

        var token = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("permanent_user", PERMANENT_PASSWORD));
        assertNull(token.getCredentials());
        var principal = (ProcurementUserDetails) token.getPrincipal();
        assertNull(principal.getPassword());
        assertTrue(token.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_MANAGER")));
        // Erasing the detached security snapshot must not erase database credentials.
        assertTrue(encoder.matches(PERMANENT_PASSWORD, users.findById(created.id()).orElseThrow().getPasswordHash()));
    }

    @Test
    void invalidPasswordSizeHasNoBcryptTruncationAndInfrastructureErrorsAreNotBadCredentials() {
        for (String invalid : new String[]{"x".repeat(73), "Ж".repeat(37), "bad\u0000password"}) {
            assertThrows(BadCredentialsException.class, () -> authentication.authenticate("unknown_user", invalid));
        }
        assertEquals(0, encoder.matches.get());
        var broken = new CredentialAuthenticationService(request -> {
            throw new InternalAuthenticationServiceException("Test database unavailable");
        });
        assertThrows(InternalAuthenticationServiceException.class,
                () -> broken.authenticate("user", PERMANENT_PASSWORD));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        @Primary
        CountingEncoder countingEncoder() {
            return new CountingEncoder();
        }

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    static final class CountingEncoder implements PasswordEncoder {
        private final PasswordEncoder delegate = new BCryptPasswordEncoder(4);
        private final AtomicInteger matches = new AtomicInteger();

        @Override
        public String encode(CharSequence rawPassword) {
            return delegate.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            matches.incrementAndGet();
            return delegate.matches(rawPassword, encodedPassword);
        }
    }

    static final class MutableClock extends Clock {
        private Instant now = NOW;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
