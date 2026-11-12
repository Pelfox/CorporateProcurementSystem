package com.team.corporate.test.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(properties = {"app.auth.rate-limit.login-ip=5", "app.auth.rate-limit.login-username=3",
        "app.auth.rate-limit.refresh-ip=5", "app.auth.rate-limit.refresh-token=2",
        "app.auth.rate-limit.password-ip=5", "app.auth.rate-limit.password-token=2"})
class RateLimitHttpTest extends AuthLifecycleTestBase {
    private static final AtomicLong WINDOWS = new AtomicLong();
    @MockitoSpyBean private PasswordEncoder encoder;

    @BeforeEach
    void newLimiterWindow() {
        clock.now = AuthTestBeans.NOW.plusSeconds(WINDOWS.incrementAndGet() * 120);
    }

    @Test
    void unknownAndKnownUsernamesHaveTheSameThresholdAndRejectionPrecedesBcrypt() throws Exception {
        activate("known_login");
        for (int i = 0; i < 3; i++) {
            json(post("/api/auth/login"), fields("username", i == 1 ? " KNOWN_LOGIN " : "known_login", "password", "wrong"))
                    .andExpect(status().isUnauthorized());
        }
        clearInvocations(encoder);
        json(post("/api/auth/login"), fields("username", "Known_Login", "password", PASSWORD))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
        verify(encoder, never()).matches(any(), any());
        clock.now = clock.now.plusSeconds(60);
        for (int i = 0; i < 3; i++) {
            json(post("/api/auth/login"), fields("username", "unknown_login", "password", "wrong"))
                    .andExpect(status().isUnauthorized());
        }
        clearInvocations(encoder);
        json(post("/api/auth/login"), fields("username", "UNKNOWN_LOGIN", "password", "wrong"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
        verify(encoder, never()).matches(any(), any());
        clock.now = clock.now.plusSeconds(60);
        json(post("/api/auth/login"), fields("username", "known_login", "password", PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void successfulLoginDoesNotEraseTheIpWindowAndSpoofedForwardedHeadersCannotBypassIt() throws Exception {
        activate("successful_login");
        for (int i = 0; i < 4; i++) {
            json(post("/api/auth/login").header("X-Forwarded-For", "198.51.100." + i),
                    fields("username", "random_" + i, "password", "wrong")).andExpect(status().isUnauthorized());
        }
        json(post("/api/auth/login"), fields("username", "successful_login", "password", PASSWORD)).andExpect(status().isOk());
        clearInvocations(encoder);
        json(post("/api/auth/login").header("X-Forwarded-For", "203.0.113.99"), fields("username", "unused", "password", "wrong"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
        verify(encoder, never()).matches(any(), any());
    }

    @Test
    void refreshAndInitialPasswordHaveIndependentSubjectLimits() throws Exception {
        var pair = activate("refresh_limited");
        var limited = limited("password_limited").passwordChange().token();
        for (int i = 0; i < 2; i++) {
            json(post("/api/auth/password/initial").header("Authorization", "Bearer " + limited), fields("newPassword", "weak"))
                    .andExpect(status().isBadRequest());
        }
        clearInvocations(encoder);
        json(post("/api/auth/password/initial").header("Authorization", "Bearer " + limited), fields("newPassword", PASSWORD))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        verify(encoder, never()).matches(any(), any());
        json(post("/api/auth/refresh"), fields("refreshToken", pair.refreshToken())).andExpect(status().isOk());
        json(post("/api/auth/refresh"), fields("refreshToken", pair.refreshToken())).andExpect(status().isUnauthorized());
        json(post("/api/auth/refresh"), fields("refreshToken", pair.refreshToken())).andExpect(status().isTooManyRequests());
    }
}
