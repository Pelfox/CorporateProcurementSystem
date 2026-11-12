package com.team.corporate.test.backend;

import com.team.corporate.backend.config.AuthRateLimitProperties;
import com.team.corporate.backend.security.AuthAttemptLimiter;
import com.team.corporate.backend.security.ClientAddressResolver;
import com.team.corporate.backend.security.ConcurrencyLimitedPasswordEncoder;
import com.team.corporate.backend.security.RateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {
    static AuthRateLimitProperties limits(int maxKeys, List<String> proxies) {
        return new AuthRateLimitProperties(Duration.ofSeconds(60), 5, 3, 5, 3, 5, 3, maxKeys, 1, proxies);
    }

    @Test
    void normalizedUsernamesShareThresholdAndClockRestoresAccessAtDeadline() {
        var clock = new AuthTestBeans.MutableClock();
        var limiter = new AuthAttemptLimiter(clock, limits(100, List.of()));
        limiter.login(" Alice ");
        limiter.login("ALICE");
        limiter.login("alice");
        assertEquals(60, assertThrows(RateLimitException.class, () -> limiter.login("alice")).retryAfter());
        clock.now = clock.now.plusSeconds(59).plusNanos(1);
        assertEquals(1, assertThrows(RateLimitException.class, () -> limiter.login("ALICE")).retryAfter());
        clock.now = clock.now.plusSeconds(1);
        assertDoesNotThrow(() -> limiter.login("alice"));
    }

    @Test
    void saturationCannotEvictLiveKeysAndTtlReleasesMemory() {
        var clock = new AuthTestBeans.MutableClock();
        var limiter = new AuthAttemptLimiter(clock, limits(2, List.of()));
        limiter.login("one");
        limiter.login("two");
        assertThrows(RateLimitException.class, () -> limiter.login("three"));
        limiter.login("one");
        limiter.login("one");
        assertThrows(RateLimitException.class, () -> limiter.login("one"));
        clock.now = clock.now.plusSeconds(60);
        assertDoesNotThrow(() -> limiter.login("three"));
    }

    @Test
    void concurrentAttemptsCannotExceedTheThreshold() throws Exception {
        var limiter = new AuthAttemptLimiter(new AuthTestBeans.MutableClock(), limits(100, List.of()));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(12)) {
            var attempts = java.util.stream.IntStream.range(0, 30).mapToObj(ignored -> executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                try {
                    limiter.login("same_user");
                    return 1;
                } catch (RateLimitException rejected) {
                    return 0;
                }
            })).toList();
            start.countDown();
            int allowed = 0;
            for (var attempt : attempts) {
                allowed += attempt.get(5, TimeUnit.SECONDS);
            }
            assertEquals(3, allowed);
        }
    }

    @Test
    void differentActionsAndSecretsDoNotShareSubjectCounters() {
        var limiter = new AuthAttemptLimiter(new AuthTestBeans.MutableClock(), limits(100, List.of()));
        for (int i = 0; i < 3; i++) {
            limiter.secret("refresh", "secret-A");
        }
        assertThrows(RateLimitException.class, () -> limiter.secret("refresh", "secret-A"));
        assertDoesNotThrow(() -> limiter.secret("initial", "secret-A"));
        assertDoesNotThrow(() -> limiter.secret("refresh", "secret-B"));
    }

    @Test
    void forwardedHeadersRequireATrustedImmediatePeerAndWalkFromTheRight() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.9");
        request.addHeader("X-Forwarded-For", "198.51.100.1");
        assertEquals("192.0.2.9", new ClientAddressResolver(limits(100, List.of())).resolve(request));
        var resolver = new ClientAddressResolver(limits(100, List.of("192.0.2.9", "192.0.2.8")));
        request.removeHeader("X-Forwarded-For");
        request.addHeader("X-Forwarded-For", "203.0.113.99, 198.51.100.1, 192.0.2.8");
        assertEquals("198.51.100.1", resolver.resolve(request));
        request.removeHeader("X-Forwarded-For");
        request.addHeader("X-Forwarded-For", "host.invalid");
        assertEquals("192.0.2.9", resolver.resolve(request));
        assertThrows(IllegalArgumentException.class, () -> new ClientAddressResolver(limits(100, List.of("hostname.invalid"))));
    }

    @Test
    void passwordWorkIsBoundedAndPermitsAreReturnedAfterSuccessOrFailure() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        PasswordEncoder delegate = new PasswordEncoder() {
            @Override public String encode(CharSequence raw) { throw new IllegalStateException("test failure"); }
            @Override public boolean matches(CharSequence raw, String hash) {
                entered.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(error);
                }
                return true;
            }
        };
        var encoder = new ConcurrencyLimitedPasswordEncoder(delegate, 1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var running = executor.submit(() -> encoder.matches("raw", "hash"));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertThrows(RateLimitException.class, () -> encoder.matches("raw", "hash"));
                assertThrows(RateLimitException.class, () -> encoder.encode("raw"));
            } finally {
                release.countDown();
            }
            assertTrue(running.get(5, TimeUnit.SECONDS));
        }
        assertThrows(IllegalStateException.class, () -> encoder.encode("raw"));
        assertTrue(encoder.matches("raw", "hash"));
    }
}
