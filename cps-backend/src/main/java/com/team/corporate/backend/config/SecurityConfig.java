package com.team.corporate.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import com.team.corporate.backend.security.SessionJwtAuthenticationConverter;
import org.springframework.http.HttpMethod;

import java.nio.charset.StandardCharsets;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SessionJwtAuthenticationConverter converter) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAuthority("SESSION_FULL")
                        .requestMatchers(HttpMethod.POST, "/api/auth/password/initial").hasAuthority("SESSION_PASSWORD_CHANGE")
                        .anyRequest().denyAll())
                // Only this explicit Bearer+JSON endpoint is exposed in C; no automatic browser credentials are used.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/password/initial"))
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((request, response, error) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.getWriter().write("{\"status\":401,\"message\":\"Недействительный токен.\"}");
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, error) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write("{\"status\":401,\"message\":\"Требуется аутентификация.\"}");
                }))
                .build();
    }
}
