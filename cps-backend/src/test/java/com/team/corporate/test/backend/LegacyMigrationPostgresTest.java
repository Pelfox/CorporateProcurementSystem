package com.team.corporate.test.backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyMigrationPostgresTest {
    @Test
    void upgradePreservesLegacyUsersAndOrdersWithoutAssigningSharedCredentials() {
        var source = new DriverManagerDataSource(System.getenv("POSTGRES_TEST_URL"),
                System.getenv().getOrDefault("POSTGRES_TEST_USER", "cps_test"),
                System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", ""));
        var jdbc = new JdbcTemplate(source);
        // Each run owns a separate schema, never the application's existing schema.
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        var config = Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration");
        try {
            config.target("2").load().migrate();
            UUID user = UUID.randomUUID();
            UUID order = UUID.randomUUID();
            jdbc.update("INSERT INTO " + schema + ".users (id, username, user_role, created_at) VALUES (?, ?, 'USER', CURRENT_TIMESTAMP)",
                    user, "legacy_user");
            jdbc.update("INSERT INTO " + schema + ".orders (id, user_id, order_status, created_at, updated_at) "
                    + "VALUES (?, ?, 'CREATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", order, user);

            config.target("latest").load().migrate();
            var row = jdbc.queryForMap("SELECT * FROM " + schema + ".users WHERE id = ?", user);
            assertEquals(user, row.get("id"));
            assertNull(row.get("password_hash"));
            assertFalse((Boolean) row.get("enabled"));
            assertTrue((Boolean) row.get("must_change_password"));
            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("UPDATE " + schema + ".users SET enabled = TRUE WHERE id = ?", user));
            assertEquals(user, jdbc.queryForObject("SELECT user_id FROM " + schema + ".orders WHERE id = ?", UUID.class, order));
            assertEquals(0, config.load().migrate().migrationsExecuted);
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }
}
