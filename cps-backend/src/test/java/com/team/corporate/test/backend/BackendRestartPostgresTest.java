package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** E20/E22/E24: real TCP, packaged application, two JVMs, persistent PostgreSQL and external key files. */
class BackendRestartPostgresTest {
    @TempDir Path directory;
    private final Set<String> secrets = new HashSet<>();

    private record Pair(String access, String refresh, String sessionExpiresAt) {
        @Override
        public String toString() {
            return "Pair[credentials=<redacted>]";
        }
    }

    @Test
    @Timeout(240)
    void lifecycleRightsAndReplayHistorySurviveRestartWhileTheLocalLimiterResets() throws Exception {
        String schema = "e2e_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        try {
            exerciseRestart(schema);
        } finally {
            // This test owns only this freshly generated schema, never the surrounding database.
            if (!schema.matches("e2e_[0-9a-f]{32}")) {
                throw new IllegalStateException("Unexpected test schema.");
            }
            try (var connection = connection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private void exerciseRestart(String schema) throws Exception {
        Path keys = Files.createDirectories(directory.resolve("keys"));
        Files.copy(JwtTestKeys.DIRECTORY.resolve("private-key.der"), keys.resolve("private-key.der"));
        Files.copy(JwtTestKeys.DIRECTORY.resolve("public-key.der"), keys.resolve("public-key.der"));
        byte[] originalKeyDigest = digest(keys.resolve("private-key.der"));
        String bootstrapPassword = secret();
        String adminPassword = secret();
        String workerPassword = secret();
        String pendingPassword = secret();
        var environment = environment(schema, keys);
        environment.put("BOOTSTRAP_ADMIN_ENABLED", "true");
        environment.put("BOOTSTRAP_ADMIN_USERNAME", "e2e.admin");
        environment.put("BOOTSTRAP_ADMIN_TEMPORARY_PASSWORD", bootstrapPassword);

        Pair admin;
        Pair rotated;
        Pair surviving;
        Pair loggedOut;
        String usedRefresh;
        String initial;
        String pending;
        String pendingId;
        String workerId;
        try (var backend = BackendProcess.start(directory.resolve("first"), environment)) {
            admin = initial(backend, loginLimited(backend, "e2e.admin", bootstrapPassword), adminPassword);
            var created = call(backend, 201, "POST", "/api/admin/users", admin.access(),
                    Map.of("username", "e2e.worker", "role", "USER"));
            workerId = created.path("id").asString();
            initial = loginLimited(backend, "e2e.worker", remember(created, "temporaryPassword"));
            call(backend, 403, "GET", "/api/auth/me", initial, null);
            Pair first = initial(backend, initial, workerPassword);
            var me = call(backend, 200, "GET", "/api/auth/me", first.access(), null);
            assertEquals(workerId, me.path("id").asString());
            assertEquals("USER", me.path("role").asString());
            assertFalse(me.has("passwordHash"));
            assertFalse(me.has("temporaryPassword"));
            usedRefresh = first.refresh();
            rotated = pair(call(backend, 200, "POST", "/api/auth/refresh", null, Map.of("refreshToken", usedRefresh)));
            assertEquals(first.sessionExpiresAt(), rotated.sessionExpiresAt());
            surviving = loginFull(backend, "e2e.worker", workerPassword);
            loggedOut = loginFull(backend, "e2e.worker", workerPassword);
            call(backend, 204, "POST", "/api/auth/logout", null, Map.of("refreshToken", loggedOut.refresh()));
            call(backend, 401, "GET", "/api/auth/me", loggedOut.access(), null);

            var another = call(backend, 201, "POST", "/api/admin/users", admin.access(),
                    Map.of("username", "e2e.pending", "role", "USER"));
            pendingId = another.path("id").asString();
            pending = loginLimited(backend, "e2e.pending", remember(another, "temporaryPassword"));
            for (int attempt = 0; attempt < 3; attempt++) {
                call(backend, 401, "POST", "/api/auth/login", null, Map.of("username", "restart.counter", "password", "invalid"));
            }
            call(backend, 429, "POST", "/api/auth/login", null, Map.of("username", "restart.counter", "password", "invalid"));
            assertDatabaseState(schema);
        }

        environment.put("BOOTSTRAP_ADMIN_ENABLED", "false");
        environment.remove("BOOTSTRAP_ADMIN_TEMPORARY_PASSWORD");
        try (var backend = BackendProcess.start(directory.resolve("second"), environment)) {
            assertTrue(MessageDigest.isEqual(originalKeyDigest, digest(keys.resolve("private-key.der"))), "Private key changed on restart");
            call(backend, 200, "GET", "/api/auth/me", admin.access(), null);
            call(backend, 200, "GET", "/api/auth/me", surviving.access(), null);
            call(backend, 200, "GET", "/api/auth/me", rotated.access(), null);
            call(backend, 401, "GET", "/api/auth/me", loggedOut.access(), null);
            call(backend, 401, "POST", "/api/auth/refresh", null, Map.of("refreshToken", loggedOut.refresh()));
            call(backend, 401, "POST", "/api/auth/password/initial", initial, Map.of("newPassword", secret()));
            call(backend, 403, "GET", "/api/auth/me", pending, null);
            Pair pendingFull = initial(backend, pending, pendingPassword);

            var refreshed = pair(call(backend, 200, "POST", "/api/auth/refresh", null, Map.of("refreshToken", surviving.refresh())));
            assertEquals(surviving.sessionExpiresAt(), refreshed.sessionExpiresAt());
            assertFalse(surviving.refresh().equals(refreshed.refresh()), "Refresh secret must rotate");
            call(backend, 401, "POST", "/api/auth/refresh", null, Map.of("refreshToken", usedRefresh));
            call(backend, 401, "GET", "/api/auth/me", rotated.access(), null);
            call(backend, 401, "POST", "/api/auth/refresh", null, Map.of("refreshToken", rotated.refresh()));
            call(backend, 401, "POST", "/api/auth/login", null, Map.of("username", "restart.counter", "password", "invalid"));

            verifyCurrentRights(backend, admin, pendingFull, pendingId, pendingPassword);
            call(backend, 204, "POST", "/api/auth/logout", null, Map.of("refreshToken", refreshed.refresh()));
            call(backend, 401, "GET", "/api/auth/me", refreshed.access(), null);
            call(backend, 401, "GET", "/api/auth/me", surviving.access(), null);
            call(backend, 401, "POST", "/api/auth/refresh", null, Map.of("refreshToken", refreshed.refresh()));
            assertEquals(workerId, call(backend, 200, "GET", "/api/admin/users/" + workerId, admin.access(), null).path("id").asString());
            assertDatabaseState(schema);
        }
        for (Path run : List.of(directory.resolve("first"), directory.resolve("second"))) {
            String logs = Files.readString(run.resolve("logs/application.log")) + Files.readString(run.resolve("console.log"));
            for (String secret : secrets) {
                assertFalse(logs.contains(secret), "Backend process logs contain credentials");
            }
        }
    }

    private void verifyCurrentRights(BackendProcess backend, Pair admin, Pair worker, String id, String password) throws Exception {
        String path = "/api/admin/users/" + id;
        call(backend, 200, "PATCH", path + "/role", admin.access(), Map.of("role", "MANAGER"));
        var manager = call(backend, 200, "GET", "/api/auth/me", worker.access(), null);
        assertEquals("MANAGER", manager.path("role").asString());
        assertTrue(manager.path("permissions").toString().contains("statistics:read"));
        call(backend, 200, "GET", "/api/statistics", worker.access(), null);
        call(backend, 200, "PATCH", path + "/role", admin.access(), Map.of("role", "USER"));
        var user = call(backend, 200, "GET", "/api/auth/me", worker.access(), null);
        assertEquals("USER", user.path("role").asString());
        assertFalse(user.path("permissions").toString().contains("statistics:read"));
        call(backend, 403, "GET", "/api/statistics", worker.access(), null);
        call(backend, 200, "PATCH", path + "/enabled", admin.access(), Map.of("enabled", false));
        call(backend, 401, "GET", "/api/auth/me", worker.access(), null);
        call(backend, 200, "PATCH", path + "/enabled", admin.access(), Map.of("enabled", true));
        call(backend, 401, "GET", "/api/auth/me", worker.access(), null);
        call(backend, 401, "POST", "/api/auth/refresh", null, Map.of("refreshToken", worker.refresh()));
        var fresh = loginFull(backend, "e2e.pending", password);
        call(backend, 200, "GET", "/api/auth/me", fresh.access(), null);
    }

    private Map<String, String> environment(String schema, Path keys) {
        String original = System.getenv("POSTGRES_TEST_URL");
        String url = original + (original.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        var environment = new HashMap<String, String>();
        environment.put("SPRING_DATASOURCE_URL", url);
        environment.put("POSTGRES_USER", System.getenv().getOrDefault("POSTGRES_TEST_USER", "cps_test"));
        environment.put("POSTGRES_PASSWORD", System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", ""));
        environment.put("SPRING_FLYWAY_DEFAULT_SCHEMA", schema);
        environment.put("JWT_PRIVATE_KEY_LOCATION", keys.resolve("private-key.der").toUri().toString());
        environment.put("JWT_PUBLIC_KEY_LOCATION", keys.resolve("public-key.der").toUri().toString());
        return environment;
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(System.getenv("POSTGRES_TEST_URL"),
                System.getenv().getOrDefault("POSTGRES_TEST_USER", "cps_test"),
                System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", ""));
    }

    private void assertDatabaseState(String schema) throws Exception {
        try (var connection = connection(); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM " + schema + ".flyway_schema_history WHERE success")) {
                assertTrue(rows.next());
                assertEquals(9, rows.getInt(1));
            }
            try (var rows = statement.executeQuery("SELECT token_hash FROM " + schema + ".refresh_tokens")) {
                int count = 0;
                while (rows.next()) {
                    assertTrue(rows.getString(1).matches("[0-9a-f]{64}"), "Only refresh hashes may be persisted");
                    count++;
                }
                assertTrue(count > 0);
            }
            try (var rows = statement.executeQuery("SELECT password_hash FROM " + schema + ".users")) {
                while (rows.next()) {
                    secrets.add(rows.getString(1));
                }
            }
        }
    }

    private String loginLimited(BackendProcess backend, String username, String password) throws Exception {
        var response = call(backend, 200, "POST", "/api/auth/login", null, Map.of("username", username, "password", password));
        assertEquals("PASSWORD_CHANGE_REQUIRED", response.path("status").asString());
        assertTrue(response.path("tokens").isNull());
        return remember(response.path("passwordChange"), "token");
    }

    private Pair loginFull(BackendProcess backend, String username, String password) throws Exception {
        var response = call(backend, 200, "POST", "/api/auth/login", null, Map.of("username", username, "password", password));
        assertEquals("AUTHENTICATED", response.path("status").asString());
        return pair(response.path("tokens"));
    }

    private Pair initial(BackendProcess backend, String token, String password) throws Exception {
        return pair(call(backend, 200, "POST", "/api/auth/password/initial", token, Map.of("newPassword", password)));
    }

    private Pair pair(JsonNode body) {
        return new Pair(remember(body, "accessToken"), remember(body, "refreshToken"), body.path("sessionExpiresAt").asString());
    }

    private JsonNode call(BackendProcess backend, int status, String method, String path, String bearer, Object body) throws Exception {
        var response = backend.request(method, path, bearer, body);
        assertEquals(status, response.status(), () -> method + " " + path + " returned an unexpected HTTP status");
        if (path.startsWith("/api/auth/") && status < 300 || path.startsWith("/api/admin/users") && status < 300) {
            assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        }
        assertTrue(response.headers().allValues("Set-Cookie").isEmpty(), "The API must not set authentication cookies");
        return response.body();
    }

    private String secret() {
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        String value = "e2e-" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        secrets.add(value);
        return value;
    }

    private String remember(JsonNode node, String field) {
        String value = node.path(field).asString();
        assertFalse(value.isBlank(), "Expected credential field is missing");
        secrets.add(value);
        return value;
    }

    private static byte[] digest(Path path) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    }
}
