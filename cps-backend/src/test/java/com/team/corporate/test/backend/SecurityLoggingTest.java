package com.team.corporate.test.backend;

import com.team.corporate.backend.dto.InitialPasswordRequest;
import com.team.corporate.core.entities.UserRole;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityLoggingTest extends AuthLifecycleTestBase {
    @Test
    void authenticationAuditAndMalformedJsonNeverLogCredentials() throws Exception {
        var root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        root.addAppender(appender);
        try {
            var created = management.createUser("logging_user", UserRole.USER);
            var limited = auth.login("logging_user", created.temporaryPassword());
            var pair = auth.initialPassword(limited.passwordChange().token(), new InitialPasswordRequest(PASSWORD));
            auth.logout(pair.refreshToken());
            String malformedSecret = "malformed-json-secret-56";
            mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content("{\"password\":\"" + malformedSecret + "\",\"username\":"))
                    .andExpect(status().isBadRequest());
            String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .collect(java.util.stream.Collectors.joining("\n"));
            for (String secret : new String[]{created.temporaryPassword(), PASSWORD, limited.passwordChange().token(),
                    pair.accessToken(), pair.refreshToken(), users.findByUsername("logging_user").orElseThrow().getPasswordHash(), malformedSecret}) {
                assertFalse(logged.contains(secret), "Security logs must not include credentials");
            }
            assertTrue(logged.contains("action=PASSWORD_INITIAL"));
            assertTrue(logged.contains("result=SUCCESS"));
        } finally {
            root.detachAppender(appender);
            appender.stop();
        }
    }
}
