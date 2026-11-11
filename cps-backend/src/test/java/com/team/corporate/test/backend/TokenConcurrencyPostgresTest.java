package com.team.corporate.test.backend;

import com.team.corporate.backend.dto.ChangePasswordRequest;
import com.team.corporate.backend.dto.InitialPasswordRequest;
import com.team.corporate.backend.security.InvalidRefreshTokenException;
import com.team.corporate.backend.services.CredentialAuthenticationService;
import com.team.corporate.backend.services.responses.AuthenticatedUser;
import com.team.corporate.backend.services.responses.TokenPairResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TokenConcurrencyPostgresTest.GateConfiguration.class)
class TokenConcurrencyPostgresTest extends AuthLifecycleTestBase {
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("POSTGRES_TEST_URL"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("POSTGRES_TEST_USER", "cps_test"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", ""));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private GateCredentials gate;

    @Test
    void databaseTimestampPrecisionDoesNotExtendTheAbsoluteDeadline() {
        clock.now = AuthTestBeans.NOW.plusNanos(123456789);
        var first = activate("deadline_precision");
        clock.now = clock.now.plusSeconds(2);
        var second = auth.refresh(first.refreshToken());
        assertEquals(first.sessionExpiresAt(), second.sessionExpiresAt());
    }

    @Test
    void concurrentRefreshOnlyRotatesOnceAndReplayRevokesTheCommittedWinner() throws Exception {
        var first = activate("parallel_refresh");
        var old = decoder.decode(first.accessToken());
        var start = new CountDownLatch(1);
        Callable<TokenPairResponse> attempt = () -> {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Refresh test timed out");
            }
            try {
                return auth.refresh(first.refreshToken());
            } catch (InvalidRefreshTokenException expected) {
                return null;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(attempt);
            var b = executor.submit(attempt);
            start.countDown();
            var resultA = a.get(15, TimeUnit.SECONDS);
            var resultB = b.get(15, TimeUnit.SECONDS);
            assertEquals(1, (resultA == null ? 0 : 1) + (resultB == null ? 0 : 1));
            var winner = resultA == null ? resultB : resultA;
            assertNotNull(winner);
            assertRejected(old);
            assertRejected(decoder.decode(winner.accessToken()));
            assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(winner.refreshToken()));
            assertEquals(2L, count("refresh_tokens"));
        }
    }

    @Test
    void concurrentInitialChangeCreatesOnlyOneFullSession() throws Exception {
        String challenge = limited("parallel_initial").passwordChange().token();
        var start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Initial password test timed out");
            }
            try {
                auth.initialPassword(challenge, new InitialPasswordRequest(PASSWORD));
                return true;
            } catch (BadCredentialsException expected) {
                return false;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(attempt);
            var b = executor.submit(attempt);
            start.countDown();
            assertEquals(1, (a.get(15, TimeUnit.SECONDS) ? 1 : 0) + (b.get(15, TimeUnit.SECONDS) ? 1 : 0));
        }
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE session_type = 'FULL'", Long.class));
        assertEquals(1L, count("refresh_tokens"));
    }

    @Test
    void logoutRacingWithRefreshLeavesNoUsableTokens() throws Exception {
        var first = activate("parallel_logout");
        var old = decoder.decode(first.accessToken());
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var rotate = executor.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Refresh/logout test timed out");
                }
                try {
                    return auth.refresh(first.refreshToken());
                } catch (InvalidRefreshTokenException expected) {
                    return null;
                }
            });
            var logout = executor.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Refresh/logout test timed out");
                }
                auth.logout(first.refreshToken());
                return null;
            });
            start.countDown();
            var next = rotate.get(15, TimeUnit.SECONDS);
            logout.get(15, TimeUnit.SECONDS);
            assertRejected(old);
            if (next != null) {
                assertRejected(decoder.decode(next.accessToken()));
                assertThrows(InvalidRefreshTokenException.class, () -> auth.refresh(next.refreshToken()));
            }
        }
    }

    @Test
    void aPasswordChangedAfterCredentialCheckPreventsTheOldLoginFromCreatingASession() throws Exception {
        var full = activate("parallel_password");
        gate.checked = new CountDownLatch(1);
        gate.release = new CountDownLatch(1);
        gate.pause = true;
        try (var executor = Executors.newSingleThreadExecutor()) {
            var login = executor.submit(() -> assertThrows(BadCredentialsException.class,
                    () -> auth.login("parallel_password", PASSWORD)));
            try {
                assertTrue(gate.checked.await(10, TimeUnit.SECONDS));
                auth.changePassword(full.accessToken(), new ChangePasswordRequest(PASSWORD, NEXT_PASSWORD));
            } finally {
                gate.pause = false;
                gate.release.countDown();
            }
            login.get(15, TimeUnit.SECONDS);
        }
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE revoked_at IS NULL", Long.class));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class GateConfiguration {
        @Bean
        @Primary
        GateCredentials gateCredentials(AuthenticationManager manager) {
            return new GateCredentials(manager);
        }
    }

    static final class GateCredentials extends CredentialAuthenticationService {
        volatile boolean pause;
        volatile CountDownLatch checked;
        volatile CountDownLatch release;

        GateCredentials(AuthenticationManager manager) {
            super(manager);
        }

        @Override
        public AuthenticatedUser authenticate(String username, String password) {
            var result = super.authenticate(username, password);
            if (pause) {
                checked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Credential check test timed out");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }
            return result;
        }
    }
}
