package com.team.corporate.core.entities;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 32)
    private SessionType sessionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public AuthSession() {
    }

    public AuthSession(User user, Instant expiresAt) {
        this(user, expiresAt, SessionType.FULL);
    }

    public AuthSession(User user, Instant expiresAt, SessionType sessionType) {
        this.user = EntityValidation.requireNonNull(user, "Пользователь");
        this.expiresAt = EntityValidation.requireNonNull(expiresAt, "Срок сессии");
        this.sessionType = EntityValidation.requireNonNull(sessionType, "Тип сессии");
    }

    @NotNull
    public SessionType getSessionType() {
        return sessionType;
    }

    @NotNull
    public UUID getId() { return id; }

    @NotNull
    public User getUser() { return user; }

    @NotNull
    public Instant getCreatedAt() { return createdAt; }

    @NotNull
    public Instant getExpiresAt() { return expiresAt; }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = EntityValidation.requireNonNull(expiresAt, "Срок сессии");
    }

    @Nullable
    public Instant getRevokedAt() { return revokedAt; }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
