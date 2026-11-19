package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditMigrationTest {
    @Test
    void migrationPreservesAndImportsLegacyOrderHistoryWithoutForeignKeyCoupling() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID order = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-11-01T12:00:00Z");
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:audit-migration;MODE=PostgreSQL")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE users (id UUID PRIMARY KEY, username VARCHAR(255))");
                statement.execute("CREATE TABLE audit_logs (id UUID PRIMARY KEY, changed_at TIMESTAMP WITH TIME ZONE, "
                        + "changed_by UUID, order_id UUID, old_status VARCHAR(40), new_status VARCHAR(40))");
            }
            try (var statement = connection.prepareStatement("INSERT INTO users VALUES (?, ?)")) {
                statement.setObject(1, actor);
                statement.setString(2, "historical-user");
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("INSERT INTO audit_logs VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setObject(1, event);
                statement.setTimestamp(2, Timestamp.from(occurredAt));
                statement.setObject(3, actor);
                statement.setObject(4, order);
                statement.setString(5, "CREATED");
                statement.setString(6, "APPROVED");
                statement.executeUpdate();
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("database/migrations/V16__create_audit_events.sql"));
            try (var statement = connection.createStatement()) {
                try (var rows = statement.executeQuery("SELECT * FROM audit_events")) {
                    assertTrue(rows.next());
                    assertEquals(event, rows.getObject("id", UUID.class));
                    assertEquals(order, rows.getObject("entity_id", UUID.class));
                    assertEquals(actor, rows.getObject("actor_id", UUID.class));
                    assertEquals("historical-user", rows.getString("actor_username"));
                    assertEquals("ORDER_STATUS_CHANGED", rows.getString("event_type"));
                    assertEquals("ORDER", rows.getString("entity_type"));
                    assertEquals(occurredAt, rows.getTimestamp("occurred_at").toInstant());
                    assertEquals("Статус заказа: CREATED → APPROVED.", rows.getString("description"));
                    assertNull(rows.getString("correlation_id"));
                    assertFalse(rows.next());
                }
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM audit_logs")) {
                    assertTrue(rows.next());
                    assertEquals(1, rows.getInt(1));
                }
                statement.executeUpdate("DELETE FROM users");
                statement.executeUpdate("DELETE FROM audit_logs");
                try (var rows = statement.executeQuery("SELECT actor_username FROM audit_events")) {
                    assertTrue(rows.next());
                    assertEquals("historical-user", rows.getString(1));
                }
            }
        }
    }
}
