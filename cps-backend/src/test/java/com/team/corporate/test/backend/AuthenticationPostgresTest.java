package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import com.team.corporate.backend.repositories.AuthSessionRepository;
import com.team.corporate.backend.repositories.RefreshTokenRepository;
import com.team.corporate.backend.repositories.adapters.UserJpaRepository;
import com.team.corporate.backend.services.UserManagementService;
import com.team.corporate.core.entities.AuthSession;
import com.team.corporate.core.entities.RefreshToken;
import com.team.corporate.core.entities.SessionType;
import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.repositories.Transactions;
import com.team.corporate.core.services.CategoriesService;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BackendMain.class, properties = "app.auth.bcrypt-strength=4")
class AuthenticationPostgresTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("POSTGRES_TEST_URL"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("POSTGRES_TEST_USER", "cps_test"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", ""));
    }

    @Autowired private UserJpaRepository users;
    @Autowired private AuthSessionRepository sessions;
    @Autowired private RefreshTokenRepository tokens;
    @Autowired private UserManagementService management;
    @Autowired private PasswordEncoder encoder;
    @Autowired private EntityManager entityManager;
    @Autowired private Transactions transactions;
    @Autowired private Flyway flyway;
    @Autowired private CategoriesService categories;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @Transactional
    void savesAndReloadsTheUserSessionTokenChainAndLimitedSession() {
        var created = management.createUser("postgres_chain", UserRole.USER);
        var user = users.findById(created.id()).orElseThrow();
        assertTrue(encoder.matches(created.temporaryPassword(), user.getPasswordHash()));
        assertTrue(user.isEnabled());
        assertTrue(user.isMustChangePassword());
        assertNotNull(user.getTemporaryPasswordExpiresAt());
        assertFalse(created.toString().contains(created.temporaryPassword()));
        var full = sessions.saveAndFlush(new AuthSession(user, Instant.now().plusSeconds(3600)));
        sessions.saveAndFlush(new AuthSession(user, Instant.now().plusSeconds(60), SessionType.PASSWORD_CHANGE));
        var token = tokens.saveAndFlush(new RefreshToken(full, "a".repeat(64), full.getExpiresAt()));
        entityManager.clear();

        var loaded = tokens.findByTokenHash("a".repeat(64)).orElseThrow();
        assertEquals(token.getId(), loaded.getId());
        assertEquals(created.id(), loaded.getSession().getUser().getId());
        assertEquals(SessionType.FULL, loaded.getSession().getSessionType());
        assertEquals(2, sessions.findAllByUserId(created.id()).size());
        assertEquals(1, tokens.findAllBySessionId(full.getId()).size());
        assertEquals(created.id(), users.findByUsername("postgres_chain").orElseThrow().getId());
        assertTrue(users.findByUsername("not_present").isEmpty());
        assertEquals(full.getId(), sessions.findByIdForUpdate(full.getId()).orElseThrow().getId());
    }

    @Test
    @Transactional
    void duplicateHashIsRejectedByTheActualDatabaseConstraint() {
        var created = management.createUser("postgres_unique", UserRole.USER);
        var user = users.findById(created.id()).orElseThrow();
        var session = sessions.saveAndFlush(new AuthSession(user, Instant.now().plusSeconds(3600)));
        tokens.saveAndFlush(new RefreshToken(session, "b".repeat(64), session.getExpiresAt()));
        assertThrows(DataIntegrityViolationException.class,
                () -> tokens.saveAndFlush(new RefreshToken(session, "b".repeat(64), session.getExpiresAt())));
    }

    @Test
    void coreAndSpringDataWritesRollBackTogether() {
        assertThrows(IllegalStateException.class, () -> transactions.execute(() -> {
            var created = management.createUser("postgres_rollback", UserRole.USER);
            categories.createCategory("postgres_rollback_category");
            var user = users.findById(created.id()).orElseThrow();
            sessions.saveAndFlush(new AuthSession(user, Instant.now().plusSeconds(3600)));
            throw new IllegalStateException("Test rollback");
        }));
        assertTrue(users.findByUsername("postgres_rollback").isEmpty());
        assertTrue(categories.getAll().stream().noneMatch(category -> category.getName().equals("postgres_rollback_category")));
    }

    @Test
    void sessionWriteLockPreventsAnotherTransactionFromAcquiringIt() throws Exception {
        var transaction = new TransactionTemplate(transactionManager);
        var sessionId = transaction.execute(status -> {
            var created = management.createUser("postgres_lock", UserRole.USER);
            return sessions.saveAndFlush(new AuthSession(users.findById(created.id()).orElseThrow(),
                    Instant.now().plusSeconds(3600))).getId();
        });
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var holder = executor.submit(() -> transaction.execute(status -> {
                sessions.findByIdForUpdate(sessionId).orElseThrow();
                locked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Lock test timed out");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
                return null;
            }));
            try {
                assertTrue(locked.await(10, TimeUnit.SECONDS));
                assertThrows(PessimisticLockingFailureException.class, () -> transaction.execute(status -> {
                    jdbc.execute("SET LOCAL lock_timeout = '200ms'");
                    sessions.findByIdForUpdate(sessionId).orElseThrow();
                    return null;
                }));
            } finally {
                release.countDown();
                holder.get(10, TimeUnit.SECONDS);
            }
        } finally {
            transaction.executeWithoutResult(status -> {
                var session = sessions.findById(sessionId).orElseThrow();
                var userId = session.getUser().getId();
                sessions.delete(session);
                sessions.flush();
                users.deleteById(userId);
            });
        }
    }

    @Test
    void repeatedMigrationDoesNotChangeAnAlreadyMigratedDatabase() {
        flyway.validate();
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }
}
