package com.team.corporate.test.backend;

import com.team.corporate.backend.dto.InitialPasswordRequest;
import com.team.corporate.backend.security.SessionJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = true)
@Import(AuthHttpBoundaryTest.ProbeConfiguration.class)
class AuthHttpBoundaryTest extends AuthLifecycleTestBase {
    @Test
    void limitedTokenOnlyAllowsInitialChangeAndNeverMeAdminOrOrdinaryOperation() throws Exception {
        var limited = limited("http_limited").passwordChange();
        for (String path : new String[]{"/api/auth/me", "/api/admin/users", "/api/users"}) {
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + limited.token()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/test/operation").header(HttpHeaders.AUTHORIZATION, "Bearer " + limited.token()))
                .andExpect(status().isForbidden());
        json(post("/api/auth/password/initial").header(HttpHeaders.AUTHORIZATION, "Bearer " + limited.token()),
                new InitialPasswordRequest("short")).andExpect(status().isBadRequest());
        json(post("/api/auth/password/initial").header(HttpHeaders.AUTHORIZATION, "Bearer " + limited.token()),
                new InitialPasswordRequest(PASSWORD)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").isString()).andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.newPassword").doesNotExist());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + limited.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullTokenUsesCurrentRoleAndLogoutImmediatelyRejectsTheSameAccess() throws Exception {
        var full = activate("http_full");
        var request = get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + full.accessToken());
        mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("http_full"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(post("/api/test/operation").header(HttpHeaders.AUTHORIZATION, "Bearer " + full.accessToken()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/test/manager").header(HttpHeaders.AUTHORIZATION, "Bearer " + full.accessToken()))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET user_role = 'MANAGER' WHERE username = 'http_full'");
        mvc.perform(get("/api/test/manager").header(HttpHeaders.AUTHORIZATION, "Bearer " + full.accessToken()))
                .andExpect(status().isOk());
        mvc.perform(request).andExpect(jsonPath("$.role").value("MANAGER"));
        json(post("/api/auth/password/initial").with(csrf()).header(HttpHeaders.AUTHORIZATION, "Bearer " + full.accessToken()),
                new InitialPasswordRequest(NEXT_PASSWORD)).andExpect(status().isForbidden());
        auth.logout(full.refreshToken());
        mvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void cryptographicAndClaimAttacksAreRejectedThroughTheRealFilter() throws Exception {
        var full = activate("http_attacks");
        var original = decoder.decode(full.accessToken());
        for (String field : List.of("iss", "aud", "purpose", "sid", "sub")) {
            var claims = new HashMap<String, Object>(original.getClaims());
            claims.put(field, field.equals("aud") ? List.of("another-client") : "invalid");
            mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + sign(claims)))
                    .andExpect(status().isUnauthorized());
        }
        for (String field : List.of("exp", "iat", "sid", "sub")) {
            var claims = new HashMap<String, Object>(original.getClaims());
            claims.remove(field);
            mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + sign(claims)))
                    .andExpect(status().isUnauthorized());
        }
        String token = full.accessToken();
        int signature = token.lastIndexOf('.') + 1;
        String tampered = token.substring(0, signature) + (token.charAt(signature) == 'A' ? 'B' : 'A') + token.substring(signature + 1);
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
        String unsigned = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + token.substring(token.indexOf('.'), token.lastIndexOf('.') + 1);
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + unsigned))
                .andExpect(status().isUnauthorized());
        String wrongAlgorithm = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + token.substring(token.indexOf('.'));
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongAlgorithm))
                .andExpect(status().isUnauthorized());
        for (String field : List.of("iat", "nbf")) {
            var claims = new HashMap<String, Object>(original.getClaims());
            claims.put(field, clock.instant().plusSeconds(1));
            mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + sign(claims)))
                    .andExpect(status().isUnauthorized());
        }
        clock.now = full.accessExpiresAt();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validUuidClaimsStillMustMatchTheServerSessionAndRolesCannotBeInjected() throws Exception {
        var alice = activate("claim_alice");
        var bob = activate("claim_bob");
        var original = decoder.decode(alice.accessToken());
        var another = decoder.decode(bob.accessToken());
        for (var replacement : List.of(Map.entry("sub", another.getSubject()),
                Map.entry("sid", another.getClaimAsString("sid")), Map.entry("sid", UUID.randomUUID().toString()))) {
            var claims = new HashMap<String, Object>(original.getClaims());
            claims.put(replacement.getKey(), replacement.getValue());
            mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + sign(claims)))
                    .andExpect(status().isUnauthorized());
        }
        var roleClaims = new HashMap<String, Object>(original.getClaims());
        roleClaims.put("role", "ADMIN");
        roleClaims.put("authorities", List.of("ROLE_ADMIN", "SESSION_FULL"));
        String injected = sign(roleClaims);
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + injected))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + injected))
                .andExpect(status().isForbidden());

        var claims = new HashMap<String, Object>(original.getClaims());
        claims.put("purpose", "PASSWORD_CHANGE");
        json(post("/api/auth/password/initial").header(HttpHeaders.AUTHORIZATION, "Bearer " + sign(claims)),
                new InitialPasswordRequest(NEXT_PASSWORD)).andExpect(status().isUnauthorized());
        var limited = limited("claim_limited").passwordChange().token();
        claims = new HashMap<>(decoder.decode(limited).getClaims());
        claims.put("purpose", "ACCESS");
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + sign(claims)))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }

        @Bean
        @Order(0)
        SecurityFilterChain probeChain(HttpSecurity http, SessionJwtAuthenticationConverter converter) throws Exception {
            return http.securityMatcher("/api/test/**")
                    .authorizeHttpRequests(requests -> requests.requestMatchers("/api/test/manager").hasRole("MANAGER")
                            .anyRequest().hasAuthority("SESSION_FULL"))
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))).build();
        }
    }

    @RestController
    static class ProbeController {
        @PostMapping("/api/test/operation")
        String operation() {
            return "ok";
        }

        @GetMapping("/api/test/manager")
        String manager() {
            return "ok";
        }
    }
}
