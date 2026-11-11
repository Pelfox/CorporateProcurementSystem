package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.InitialPasswordRequest;
import com.team.corporate.backend.security.SessionPrincipal;
import com.team.corporate.backend.services.AuthService;
import com.team.corporate.backend.services.responses.TokenPairResponse;
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

/** Minimal C dependencies; the rest of the authentication/admin HTTP contract belongs to D. */
@RestController
@RequestMapping("/api/auth")
public class SessionController {
    private final AuthService auth;

    public SessionController(AuthService auth) {
        this.auth = auth;
    }

    public record MeResponse(UUID id, String username, UserRole role) {
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal SessionPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new MeResponse(principal.userId(), principal.username(), principal.role()));
    }

    @PostMapping("/password/initial")
    public ResponseEntity<TokenPairResponse> initialPassword(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                            @Valid @RequestBody InitialPasswordRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(auth.initialPassword(authorization.substring(7), request));
    }
}
