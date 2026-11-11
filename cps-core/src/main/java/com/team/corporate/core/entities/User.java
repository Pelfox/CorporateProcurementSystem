package com.team.corporate.core.entities;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    // Legacy users remain disabled until an administrator provisions credentials.
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword;

    @Column(name = "temporary_password_expires_at")
    private Instant temporaryPasswordExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public User() {
    }

    public User(@NotNull String username, @NotNull String passwordHash, @NotNull Instant temporaryPasswordExpiresAt, @NotNull UserRole userRole) {
        this.username = EntityValidation.requireText(username, "Имя пользователя");
        this.passwordHash = EntityValidation.requireText(passwordHash, "Хэш пароля пользователя");
        this.temporaryPasswordExpiresAt = EntityValidation.requireNonNull(temporaryPasswordExpiresAt, "Срок временного пароля");
        this.userRole = EntityValidation.requireNonNull(userRole, "Роль");

        this.mustChangePassword = true;
        this.enabled = true;
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NotNull String username) {
        this.username = EntityValidation.requireText(username, "Имя пользователя");
    }

    @Nullable
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(@NotNull String passwordHash) {
        this.passwordHash = EntityValidation.requireText(passwordHash, "Хэш пароля пользователя");
    }

    @NotNull
    public Boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(@NotNull Boolean mustChangePassword) {
        this.mustChangePassword = EntityValidation.requireNonNull(mustChangePassword, "Пароль требует смены");
    }

    @Nullable
    public Instant getTemporaryPasswordExpiresAt() {
        return temporaryPasswordExpiresAt;
    }

    @NotNull
    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(@NotNull UserRole userRole) {
        this.userRole = EntityValidation.requireNonNull(userRole, "Роль");
    }

    @NotNull
    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(@NotNull Boolean enabled) {
        this.enabled = EntityValidation.requireNonNull(enabled, "Статус учётной записи");
    }

    @Nullable
    public Instant getCreatedAt() {
        return createdAt;
    }
}
