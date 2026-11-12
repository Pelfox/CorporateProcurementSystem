package com.team.corporate.test.backend;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Runs the packaged backend in a separate JVM. No test controllers or in-process authentication shortcuts. */
final class BackendProcess implements AutoCloseable {
    record Reply(int status, JsonNode body, java.net.http.HttpHeaders headers) {
        @Override
        public String toString() {
            return "Reply[status=" + status + ", body=<redacted>]";
        }
    }

    private final Process process;
    private final HttpClient client;
    private final URI base;
    private final ObjectMapper mapper = new ObjectMapper();

    private BackendProcess(Process process, int port) {
        this.process = process;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        this.base = URI.create("http://127.0.0.1:" + port);
    }

    static BackendProcess start(Path directory, Map<String, String> environment) throws Exception {
        Files.createDirectories(directory);
        int port;
        try (var socket = new ServerSocket(0, 0, InetAddress.ofLiteral("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var jar = System.getProperty("cps.backend.jar");
        if (jar == null || !Files.isRegularFile(Path.of(jar))) {
            throw new IllegalStateException("Запускайте процессную проверку задачей :cps-backend:postgresTest.");
        }
        var builder = new ProcessBuilder(java, "-jar", jar, "--server.address=127.0.0.1", "--server.port=" + port,
                "--spring.main.banner-mode=off", "--app.auth.bcrypt-strength=4",
                "--app.auth.rate-limit.login-username=3", "--app.auth.rate-limit.login-ip=1000",
                "--app.auth.rate-limit.window=PT10M");
        builder.directory(directory.toFile());
        builder.environment().putAll(environment);
        builder.redirectErrorStream(true).redirectOutput(directory.resolve("console.log").toFile());
        var backend = new BackendProcess(builder.start(), port);
        try {
            backend.awaitReady();
            return backend;
        } catch (Exception | AssertionError failure) {
            backend.close();
            throw failure;
        }
    }

    Reply request(String method, String path, String bearer, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(15));
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = null;
        if (!response.body().isBlank()) {
            try {
                json = mapper.readTree(response.body());
            } catch (RuntimeException malformed) {
                // Parser exceptions can quote the response, including credentials.
                throw new AssertionError("Backend returned invalid JSON for " + method + " " + path);
            }
        }
        return new Reply(response.statusCode(), json, response.headers());
    }

    private void awaitReady() throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(75).toNanos();
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new AssertionError("Backend exited before readiness, exit code " + process.exitValue());
            }
            try {
                if (request("GET", "/api/auth/me", null, null).status() == 401) {
                    return;
                }
            } catch (IOException notReady) {
                // The connector is not listening yet; bounded startup polling only.
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Backend did not start within 75 seconds.");
    }

    @Override
    public void close() throws InterruptedException {
        process.destroy();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Test backend process did not terminate.");
                }
            }
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw interrupted;
        } finally {
            client.close();
        }
    }
}
