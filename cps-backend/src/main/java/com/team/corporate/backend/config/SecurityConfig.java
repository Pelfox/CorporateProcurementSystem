package com.team.corporate.backend.config;

import com.team.corporate.backend.http.SecurityErrorWriter;
import com.team.corporate.backend.security.AuthAttemptLimiter;
import com.team.corporate.backend.security.AuthRateLimitFilter;
import com.team.corporate.backend.security.ClientAddressResolver;
import com.team.corporate.backend.security.SessionJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(AuthRateLimitProperties.class)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SessionJwtAuthenticationConverter converter,
                                            AuthAttemptLimiter limiter, ClientAddressResolver addresses,
                                            SecurityErrorWriter errors, UrlBasedCorsConfigurationSource cors) throws Exception {
        return http
                .cors(configuration -> configuration.configurationSource(cors))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAuthority("SESSION_FULL")
                        .requestMatchers(HttpMethod.POST, "/api/auth/password/initial").hasAuthority("SESSION_PASSWORD_CHANGE")
                        .requestMatchers(HttpMethod.POST, "/api/auth/password/change", "/api/auth/logout-all").hasAuthority("SESSION_FULL")
                        .requestMatchers("/api/admin/users", "/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/statistics", "/api/statistics/**", "/api/exports/csv").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/*").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders", "/api/orders/**").hasAuthority("SESSION_FULL")
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/*/items").hasAuthority("SESSION_FULL")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*", "/api/orders/*/items/*").hasAuthority("SESSION_FULL")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/notes").hasAuthority("SESSION_FULL")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/*/items/*").hasAuthority("SESSION_FULL")
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/*", "/api/products", "/api/products/*",
                                "/api/warehouses", "/api/warehouses/*", "/api/warehouses/*/inventory", "/api/inventory", "/api/inventory/*")
                            .hasAuthority("SESSION_FULL")
                        .requestMatchers(HttpMethod.POST, "/api/categories", "/api/products", "/api/warehouses").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/*", "/api/products/*", "/api/warehouses/*",
                                "/api/warehouses/*/inventory/*").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/*", "/api/products/*", "/api/warehouses/*", "/api/inventory/*")
                            .hasAnyRole("MANAGER", "ADMIN")
                        // Legacy account CRUD under /api/users remains closed.
                        .anyRequest().denyAll())
                // Explicit Bearer headers/JSON secrets only. Revisit if browser cookies/Basic are introduced.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((request, response, error) -> errors.write(request, response, 401, "Недействительный токен."))
                        .accessDeniedHandler((request, response, error) -> errors.write(request, response, 403, "Недостаточно прав.")))
                .addFilterBefore(new AuthRateLimitFilter(limiter, addresses, errors), BearerTokenAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, error) -> errors.write(request, response, 401, "Требуется аутентификация."))
                        .accessDeniedHandler((request, response, error) -> errors.write(request, response, 403, "Недостаточно прав.")))
                .build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(@Value("${app.web.allowed-origins:http://localhost:5173}") List<String> origins) {
        if (origins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalArgumentException("CORS требует явные origin.");
        }
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Retry-After"));
        configuration.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
