package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.InitialPasswordRequest;
import com.team.corporate.backend.dto.ChangePasswordRequest;
import com.team.corporate.backend.dto.LoginRequest;
import com.team.corporate.backend.dto.RefreshRequest;
import com.team.corporate.backend.security.AuthAttemptLimiter;
import com.team.corporate.backend.security.Permissions;
import com.team.corporate.backend.security.SecurityAudit;
import com.team.corporate.backend.security.SessionPrincipal;
import com.team.corporate.backend.services.AuthService;
import com.team.corporate.backend.services.responses.TokenPairResponse;
import com.team.corporate.backend.services.responses.LoginResponse;
import com.team.corporate.core.entities.UserRole;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;

@RestController
@RequestMapping("/api/auth")
public class SessionController {
    private final AuthService auth;
    private final AuthAttemptLimiter limiter;

    public SessionController(AuthService auth, AuthAttemptLimiter limiter) {
        this.auth = auth;
        this.limiter = limiter;
    }

    public record MeResponse(UUID id, String username, UserRole role, List<String> permissions) {
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SESSION_FULL')")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal SessionPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new MeResponse(principal.userId(), principal.username(), principal.role(), Permissions.forRole(principal.role())));
    }

    @PostMapping("/password/initial")
    @PreAuthorize("hasAuthority('SESSION_PASSWORD_CHANGE')")
    public ResponseEntity<TokenPairResponse> initialPassword(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                            @Valid @RequestBody InitialPasswordRequest request) {
        limiter.secret("initial", authorization);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(auth.initialPassword(authorization.substring(7), request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        limiter.login(request.username());
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.login(request.username(), request.password()));
        } catch (AuthenticationException rejected) {
            SecurityAudit.record("LOGIN", null, null, "REJECTED");
            throw rejected;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        limiter.secret("refresh", request.refreshToken());
        // Direct invocation: an outer transaction must not roll back replay-triggered revocation.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        limiter.secret("logout", request.refreshToken());
        auth.logout(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/logout-all")
    @PreAuthorize("hasAuthority('SESSION_FULL')")
    public ResponseEntity<Void> logoutAll(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        auth.logoutAll(authorization.substring(7));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/password/change")
    @PreAuthorize("hasAuthority('SESSION_FULL')")
    public ResponseEntity<Void> change(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        limiter.secret("change", authorization);
        auth.changePassword(authorization.substring(7), request);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
