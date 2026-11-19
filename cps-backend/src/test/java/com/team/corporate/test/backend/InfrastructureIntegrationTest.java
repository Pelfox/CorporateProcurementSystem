package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import com.team.corporate.backend.entities.UserCredentials;
import com.team.corporate.backend.filter.CorrelationIdFilter;
import com.team.corporate.backend.repositories.SpringUsersRepository;
import com.team.corporate.backend.repositories.UserCredentialsRepository;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = BackendMain.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:backend-infrastructure;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "cps.security.bootstrap.username=", "cps.security.bootstrap.password=",
        "cps.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Import(InfrastructureIntegrationTest.HealthTestConfig.class)
class InfrastructureIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Autowired private MockMvc mvc;
    @Autowired private ProbeHealthIndicator probe;
    @Autowired private HealthContributorRegistry health;
    @Autowired private SpringUsersRepository users;
    @Autowired private UserCredentialsRepository credentials;
    @Autowired private PasswordEncoder passwords;

    @BeforeEach
    void resetHealth() { probe.down.set(false); }

    @Test
    void swaggerUiAndItsAssetsArePublicAndPointToThisApplicationsContract() throws Exception {
        var redirect = mvc.perform(get("/swagger-ui.html")).andReturn();
        assertEquals(302, redirect.getResponse().getStatus());
        assertEquals("/swagger-ui/index.html", redirect.getResponse().getRedirectedUrl());
        for (String path : List.of("/swagger-ui/index.html", "/swagger-ui/swagger-ui-bundle.js", "/swagger-ui/swagger-initializer.js")) {
            var result = mvc.perform(get(path)).andReturn();
            assertEquals(200, result.getResponse().getStatus(), path);
            assertNotNull(result.getResponse().getHeader(CorrelationIdFilter.HEADER));
        }
        var config = mvc.perform(get("/v3/api-docs/swagger-config")).andReturn();
        assertEquals(200, config.getResponse().getStatus());
        assertEquals("/v3/api-docs", json(config).get("url").asString());
    }

    @Test
    void generatedContractDescribesBearerAuthenticationAndPublicTokenOperations() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        var document = json(result);
        assertTrue(document.get("openapi").asString().startsWith("3."));
        var bearer = document.get("components").get("securitySchemes").get("bearerAuth");
        assertEquals("http", bearer.get("type").asString());
        assertEquals("bearer", bearer.get("scheme").asString());
        assertEquals("JWT", bearer.get("bearerFormat").asString());
        assertTrue(document.get("security").get(0).has("bearerAuth"));
        var paths = document.get("paths");
        for (String path : List.of("/api/auth/login", "/api/auth/refresh", "/api/auth/logout")) {
            assertEquals(0, paths.get(path).get("post").get("security").size(), path);
        }
        assertNotNull(paths.get("/api/auth/me").get("get"));
        assertNotNull(paths.get("/api/admin/audit").get("get"));
        assertNotNull(paths.get("/api/admin/users").get("post").get("responses").get("201"));
        assertNotNull(paths.get("/api/auth/password").get("put").get("responses").get("204"));
        var schemas = document.get("components").get("schemas");
        assertNotNull(schemas.get("LoginRequest").get("properties").get("password"));
        assertNotNull(schemas.get("TokenResponse").get("properties").get("mustChangePassword"));
        assertNotNull(schemas.get("UserResponse").get("properties").get("fullName"));
        for (String schemaName : List.of("ProductResponse", "CategoryProductCountResponse")) {
            var categoryId = schemas.get(schemaName).get("properties").get("categoryId");
            assertEquals("uuid", categoryId.get("format").asString());
            if (document.get("openapi").asString().startsWith("3.0")) {
                assertTrue(categoryId.get("nullable").asBoolean(), schemaName);
            } else {
                assertTrue(categoryId.get("type").isArray(), schemaName);
                assertTrue(categoryId.get("type").toString().contains("\"null\""), schemaName);
                assertTrue(categoryId.get("type").toString().contains("\"string\""), schemaName);
            }
        }
        assertFalse(result.getResponse().getContentAsString().contains("passwordHash"));
        assertFalse(paths.has("/actuator/health"));
        var yaml = mvc.perform(get("/v3/api-docs.yaml")).andReturn();
        assertEquals(200, yaml.getResponse().getStatus());
        assertTrue(yaml.getResponse().getContentAsString().startsWith("openapi:"));
    }

    @Test
    void healthIsPublicAggregatesDatabaseAndHidesDetailsEvenFromAdministrators() throws Exception {
        assertNotNull(health.getContributor("db"));
        var anonymous = mvc.perform(get("/actuator/health")).andReturn();
        assertEquals(200, anonymous.getResponse().getStatus());
        assertEquals("UP", json(anonymous).get("status").asString());
        assertFalse(json(anonymous).has("components"));
        assertFalse(json(anonymous).has("details"));
        var admin = mvc.perform(get("/actuator/health").with(user("admin").roles("ADMIN"))).andReturn();
        assertEquals(json(anonymous), json(admin));
        assertNotNull(anonymous.getResponse().getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void unhealthyContributorProduces503WithoutExposingInternalDetails() throws Exception {
        probe.down.set(true);
        try {
            var result = mvc.perform(get("/actuator/health")).andReturn();
            assertEquals(503, result.getResponse().getStatus());
            assertEquals("DOWN", json(result).get("status").asString());
            assertFalse(json(result).has("components"));
            assertFalse(result.getResponse().getContentAsString().contains("private-db-host"));
            assertNotNull(result.getResponse().getHeader(CorrelationIdFilter.HEADER));
        } finally {
            probe.down.set(false);
        }
    }

    @Test
    void staleBearerHeadersDoNotBreakPublicDocumentationOrContainerHealthChecks() throws Exception {
        for (String path : List.of("/v3/api-docs", "/actuator/health", "/swagger-ui/index.html")) {
            var result = mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-or-expired"))
                    .andReturn();
            assertEquals(200, result.getResponse().getStatus(), path);
        }
    }

    @Test
    void documentationDoesNotOpenOtherActuatorOrAdministrativeEndpoints() throws Exception {
        for (String path : List.of("/actuator", "/actuator/env", "/actuator/beans", "/actuator/health/db")) {
            assertEquals(401, mvc.perform(get(path)).andReturn().getResponse().getStatus(), path);
            assertEquals(403, mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                    .andReturn().getResponse().getStatus(), path);
        }
        for (String path : List.of("/api/admin/users", "/api/admin/audit")) {
            assertEquals(401, mvc.perform(get(path)).andReturn().getResponse().getStatus(), path);
            assertEquals(403, mvc.perform(get(path).with(user("employee").roles("USER")))
                    .andReturn().getResponse().getStatus(), path);
        }
    }

    @Test
    void temporaryAdministratorCanReadDocumentationButStillMustChangePassword() throws Exception {
        var admin = users.save(new User("temporary-infrastructure-admin", UserRole.ADMIN));
        credentials.save(new UserCredentials(admin, passwords.encode("temporary-password-123")));
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"temporary-infrastructure-admin\",\"password\":\"temporary-password-123\"}"))
                .andReturn();
        assertEquals(200, login.getResponse().getStatus());
        assertTrue(json(login).get("mustChangePassword").asBoolean());
        String bearer = "Bearer " + json(login).get("accessToken").asString();
        assertEquals(200, mvc.perform(get("/v3/api-docs").header(HttpHeaders.AUTHORIZATION, bearer))
                .andReturn().getResponse().getStatus());
        var denied = mvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer)).andReturn();
        assertEquals(403, denied.getResponse().getStatus());
        assertEquals("PASSWORD_CHANGE_REQUIRED", json(denied).get("code").asString());
    }

    private static JsonNode json(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HealthTestConfig {
        @Bean
        ProbeHealthIndicator probeHealthIndicator() { return new ProbeHealthIndicator(); }
    }

    static class ProbeHealthIndicator implements HealthIndicator {
        final AtomicBoolean down = new AtomicBoolean();

        @Override
        public Health health() {
            return (down.get() ? Health.down() : Health.up()).withDetail("internalHost", "private-db-host").build();
        }
    }
}
