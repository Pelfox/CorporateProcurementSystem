package com.team.corporate.backend.controllers;

import com.team.corporate.backend.services.AdminUsersService;
import com.team.corporate.backend.services.responses.CreateUserResponse;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUsersController {
    private final AdminUsersService users;

    public AdminUsersController(AdminUsersService users) {
        this.users = users;
    }

    public record Create(@NotNull @Size(min = 1, max = 256) String username, @NotNull UserRole role) {
    }

    public record Enabled(@NotNull Boolean enabled) {
    }

    public record Role(@NotNull UserRole role) {
    }

    public record View(UUID id, String username, UserRole role, boolean enabled, boolean mustChangePassword,
                       Instant temporaryPasswordExpiresAt, Instant createdAt) {
        static View from(User user) {
            return new View(user.getId(), user.getUsername(), user.getUserRole(), user.isEnabled(),
                    user.isMustChangePassword(), user.getTemporaryPasswordExpiresAt(), user.getCreatedAt());
        }
    }

    @GetMapping
    public ResponseEntity<List<View>> list() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.list().stream().map(View::from).toList());
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> create(@Valid @RequestBody Create request) {
        var result = users.create(request.username(), request.role());
        return ResponseEntity.created(URI.create("/api/admin/users/" + result.id()))
                .cacheControl(CacheControl.noStore()).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<View> get(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(View.from(users.get(id)));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<View> enabled(@PathVariable UUID id, @Valid @RequestBody Enabled request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(View.from(users.enabled(id, request.enabled())));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<View> role(@PathVariable UUID id, @Valid @RequestBody Role request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(View.from(users.role(id, request.role())));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<CreateUserResponse> reset(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.reset(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        users.delete(id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
