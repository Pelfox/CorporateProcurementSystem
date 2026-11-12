package com.team.corporate.backend.security;

import com.team.corporate.backend.http.SecurityErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/** Registered only in the security chain, before JWT decoding and before controller/BCrypt work. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Map<String, String> ACTIONS = Map.of(
            "/api/auth/login", "login", "/api/auth/refresh", "refresh", "/api/auth/logout", "logout",
            "/api/auth/password/initial", "initial", "/api/auth/password/change", "change");
    private final AuthAttemptLimiter limiter;
    private final ClientAddressResolver addresses;
    private final SecurityErrorWriter errors;

    public AuthRateLimitFilter(AuthAttemptLimiter limiter, ClientAddressResolver addresses, SecurityErrorWriter errors) {
        this.limiter = limiter;
        this.addresses = addresses;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String action = ACTIONS.get(request.getServletPath().isEmpty() ? request.getRequestURI() : request.getServletPath());
        try {
            if (action != null && request.getMethod().equals("POST")) {
                limiter.ip(action, addresses.resolve(request));
            }
        } catch (RateLimitException limited) {
            response.setHeader("Retry-After", Long.toString(limited.retryAfter()));
            errors.write(request, response, 429, limited.getMessage());
            return;
        }
        chain.doFilter(request, response);
    }
}
