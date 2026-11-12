package com.team.corporate.test.backend;

import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.security.SessionPrincipal;
import com.team.corporate.backend.services.AdminUsersService;
import com.team.corporate.backend.services.responses.TokenPairResponse;
import com.team.corporate.core.entities.SessionType;
import com.team.corporate.core.entities.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministrationConcurrencyPostgresTest extends AuthLifecycleTestBase {
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("POSTGRES_TEST_URL"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("POSTGRES_TEST_USER", "cps_test"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", ""));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private AdminUsersService administration;

    private SessionPrincipal principal(String name, TokenPairResponse pair) {
        var user = users.findByUsername(name).orElseThrow();
        return new SessionPrincipal(user.getId(), sessionId(pair.accessToken()), name, UserRole.ADMIN, SessionType.FULL);
    }

    private void context(SessionPrincipal principal) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("SESSION_FULL"))));
        SecurityContextHolder.setContext(context);
    }

    enum Removal { DEMOTE, BLOCK, DELETE }

    @ParameterizedTest
    @EnumSource(Removal.class)
    void simultaneousChangesCannotRemoveTheLastActiveAdmin(Removal removal) throws Exception {
        var a = activate("parallel_admin_a");
        var b = activate("parallel_admin_b");
        jdbc.update("UPDATE users SET user_role = 'ADMIN'");
        var pa = principal("parallel_admin_a", a);
        var pb = principal("parallel_admin_b", b);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(pa, pb).stream().map(principal -> executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                context(principal);
                try {
                    switch (removal) {
                        case DEMOTE -> administration.role(principal.userId(), UserRole.MANAGER);
                        case BLOCK -> administration.enabled(principal.userId(), false);
                        case DELETE -> administration.delete(principal.userId());
                    }
                    return true;
                } catch (ApiException conflict) {
                    assertEquals(409, conflict.status().value());
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            start.countDown();
            int changed = 0;
            for (var result : results) {
                changed += result.get(10, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertEquals(1, changed);
            assertEquals(1, users.countByUserRoleAndEnabledTrue(UserRole.ADMIN));
        }
    }

    @Test
    void staleAdminPrincipalIsRecheckedInsideTheMembershipTransaction() {
        var pair = activate("stale_admin");
        var principal = principal("stale_admin", pair);
        // The principal was verified before another transaction committed demotion.
        context(principal);
        try {
            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> administration.create("unauthorized_worker", UserRole.ADMIN));
            assertTrue(users.findByUsername("unauthorized_worker").isEmpty());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
