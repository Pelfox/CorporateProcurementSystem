package com.team.corporate.test.backend;

import com.team.corporate.backend.services.AdminUsersService;
import com.team.corporate.backend.services.BootstrapAdminService;
import com.team.corporate.backend.services.responses.TokenPairResponse;
import com.team.corporate.core.entities.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = true)
class AdministrationHttpTest extends AuthLifecycleTestBase {
    @Autowired private AdminUsersService administration;
    @Autowired private BootstrapAdminService bootstrap;

    private TokenPairResponse role(String name, UserRole role) {
        var pair = activate(name);
        jdbc.update("UPDATE users SET user_role = ? WHERE username = ?", role.name(), name);
        return pair;
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, TokenPairResponse pair) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + pair.accessToken());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void rolesHaveExplicitCatalogStatisticsExportAndAccountRights(UserRole role) throws Exception {
        var pair = role("matrix_" + role.name().toLowerCase(), role);
        mvc.perform(as(get("/api/categories"), pair)).andExpect(status().isOk());
        mvc.perform(as(get("/api/statistics"), pair)).andExpect(status().is(role.canManageProcurement() ? 200 : 403));
        mvc.perform(as(get("/api/exports/csv"), pair)).andExpect(status().is(role.canManageProcurement() ? 200 : 403));
        mvc.perform(as(get("/api/admin/users"), pair)).andExpect(status().is(role == UserRole.ADMIN ? 200 : 403));
        json(as(post("/api/categories"), pair), fields("name", "matrix category"))
                .andExpect(status().is(role.canManageProcurement() ? 201 : 403));
        json(as(post("/api/admin/users"), pair), fields("username", "new_worker", "role", "USER"))
                .andExpect(status().is(role == UserRole.ADMIN ? 201 : 403));
        mvc.perform(as(get("/api/users"), pair)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/unlisted"), pair)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/auth/me"), pair)).andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.permissions").isArray()).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void anonymousAndLimitedTokensCannotReachBusinessOrAdministration() throws Exception {
        for (String path : new String[]{"/api/categories", "/api/orders", "/api/statistics", "/api/admin/users", "/api/auth/me"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(header().doesNotExist("Location")).andExpect(header().doesNotExist("Set-Cookie"));
        }
        var limited = limited("limited_http").passwordChange().token();
        for (String path : new String[]{"/api/categories", "/api/orders", "/api/statistics", "/api/admin/users", "/api/auth/me"}) {
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + limited)).andExpect(status().isForbidden());
        }
        json(post("/api/auth/refresh"), fields("refreshToken", "unknown" )).andExpect(status().isUnauthorized());
        json(post("/api/auth/logout"), fields("refreshToken", "unknown" )).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void completeAdministrativeProvisioningAndSessionLifecycleUsesOnlyHttp() throws Exception {
        var admin = role("http_admin", UserRole.ADMIN);
        var created = tree(json(as(post("/api/admin/users"), admin), fields("username", "  Worker.Name  ", "role", "USER"))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store")));
        assertThat(created.path("username").asString()).isEqualTo("worker.name");
        var login = tree(json(post("/api/auth/login"), fields("username", "WORKER.NAME", "password", created.path("temporaryPassword").asString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")));
        assertThat(login.path("status").asString()).isEqualTo("PASSWORD_CHANGE_REQUIRED");
        assertThat(login.path("tokens").isNull()).isTrue();
        var pair = tree(json(post("/api/auth/password/initial").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + login.path("passwordChange").path("token").asString()), fields("newPassword", PASSWORD))
                .andExpect(status().isOk()));
        String access = pair.path("accessToken").asString();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(created.path("id").asString()));
        var rotated = tree(json(post("/api/auth/refresh"), fields("refreshToken", pair.path("refreshToken").asString()))
                .andExpect(status().isOk()));
        json(post("/api/auth/logout"), fields("refreshToken", rotated.path("refreshToken").asString()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access)).andExpect(status().isUnauthorized());
        json(post("/api/auth/refresh"), fields("refreshToken", rotated.path("refreshToken").asString())).andExpect(status().isUnauthorized());
        String listing = mvc.perform(as(get("/api/admin/users"), admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(listing).doesNotContain("passwordHash", "temporaryPassword\"", created.path("temporaryPassword").asString(), "$2a$");
        mvc.perform(as(get("/api/admin/users/" + created.path("id").asString()), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void blockUnblockResetAndRoleChangesAffectPreviouslyIssuedTokens() throws Exception {
        var admin = role("control_admin", UserRole.ADMIN);
        var user = role("control_user", UserRole.MANAGER);
        UUID userId = users.findByUsername("control_user").orElseThrow().getId();
        String route = "/api/admin/users/" + userId;
        json(as(patch(route + "/role"), admin), fields("role", "USER")).andExpect(status().isOk());
        mvc.perform(as(get("/api/statistics"), user)).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/auth/me"), user)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));
        json(as(patch(route + "/enabled"), admin), fields("enabled", false)).andExpect(status().isOk());
        mvc.perform(as(get("/api/auth/me"), user)).andExpect(status().isUnauthorized());
        json(as(patch(route + "/enabled"), admin), fields("enabled", true)).andExpect(status().isOk());
        mvc.perform(as(get("/api/auth/me"), user)).andExpect(status().isUnauthorized());
        json(post("/api/auth/refresh"), fields("refreshToken", user.refreshToken())).andExpect(status().isUnauthorized());
        var reset = tree(mvc.perform(as(post(route + "/reset-password"), admin)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")));
        json(post("/api/auth/login"), fields("username", "control_user", "password", PASSWORD)).andExpect(status().isUnauthorized());
        var first = tree(json(post("/api/auth/login"), fields("username", "control_user", "password", reset.path("temporaryPassword").asString()))
                .andExpect(status().isOk()));
        mvc.perform(as(post(route + "/reset-password"), admin)).andExpect(status().isOk());
        json(post("/api/auth/password/initial").header(HttpHeaders.AUTHORIZATION, "Bearer " + first.path("passwordChange").path("token").asString()),
                fields("newPassword", PASSWORD)).andExpect(status().isUnauthorized());
    }

    @Test
    void lastAdminIsProtectedAndUnlinkedAccountsCanBeDeleted() throws Exception {
        var admin = role("last_admin", UserRole.ADMIN);
        UUID adminId = users.findByUsername("last_admin").orElseThrow().getId();
        String route = "/api/admin/users/" + adminId;
        json(as(patch(route + "/enabled"), admin), fields("enabled", false)).andExpect(status().isConflict());
        json(as(patch(route + "/role"), admin), fields("role", "MANAGER")).andExpect(status().isConflict());
        mvc.perform(as(delete(route), admin)).andExpect(status().isConflict());
        var user = activate("deletable");
        UUID id = users.findByUsername("deletable").orElseThrow().getId();
        mvc.perform(as(delete("/api/admin/users/" + id), admin)).andExpect(status().isNoContent());
        mvc.perform(as(get("/api/auth/me"), user)).andExpect(status().isUnauthorized());
        assertThat(users.findById(id)).isEmpty();
    }

    @Test
    void ordinaryPasswordChangeAndLogoutAllRequireFullSessionAndRevokeEverySession() throws Exception {
        var user = activate("password_http");
        var second = auth.login("password_http", PASSWORD).tokens();
        json(as(post("/api/auth/password/change"), user), fields("currentPassword", "wrong", "newPassword", NEXT_PASSWORD))
                .andExpect(status().isUnauthorized());
        json(as(post("/api/auth/password/change"), user), fields("currentPassword", PASSWORD, "newPassword", NEXT_PASSWORD))
                .andExpect(status().isNoContent());
        mvc.perform(as(get("/api/auth/me"), second)).andExpect(status().isUnauthorized());
        var fresh = auth.login("password_http", NEXT_PASSWORD).tokens();
        mvc.perform(as(post("/api/auth/logout-all"), fresh)).andExpect(status().isNoContent());
        mvc.perform(as(get("/api/auth/me"), fresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void csrfContractAndCorsAllowExplicitReactOriginWithoutAutomaticCredentials() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
        json(post("/api/auth/login"), fields("username", "absent", "password", "wrong"))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void serviceAuthorizationCannotBeBypassedAndBootstrapDoesNotOverwriteAnAdmin() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> administration.create("intruder", UserRole.ADMIN));
        bootstrap.provision("bootstrap_admin", () -> PASSWORD);
        var first = users.findByUsername("bootstrap_admin").orElseThrow();
        assertThat(first.isMustChangePassword()).isTrue();
        bootstrap.provision("other_admin", () -> { throw new AssertionError("Existing ADMIN must not trigger credential retrieval"); });
        assertThat(users.findByUsername("other_admin")).isEmpty();
    }

    @Test
    void duplicateAndInvalidAccountRequestsHaveStableClientErrors() throws Exception {
        var admin = role("validation_admin", UserRole.ADMIN);
        json(as(post("/api/admin/users"), admin), fields("username", "worker.dup", "role", "USER"))
                .andExpect(status().isCreated());
        json(as(post("/api/admin/users"), admin), fields("username", " WORKER.DUP ", "role", "MANAGER"))
                .andExpect(status().isConflict());
        json(as(post("/api/admin/users"), admin), fields("username", "кириллица", "role", "USER"))
                .andExpect(status().isBadRequest());
        json(as(post("/api/admin/users"), admin), fields("username", "valid", "role", "OWNER"))
                .andExpect(status().isBadRequest());
        json(as(post("/api/admin/users"), admin), fields("username", "valid", "role", "USER", "passwordHash", "untrusted"))
                .andExpect(status().isBadRequest());
    }
}
