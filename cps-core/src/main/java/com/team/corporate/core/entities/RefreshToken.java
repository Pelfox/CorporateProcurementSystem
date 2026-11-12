package com.team.corporate.core.entities;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private AuthSession session;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public RefreshToken() {
    }

    public RefreshToken(AuthSession session, String tokenHash, Instant expiresAt) {
        this(session, tokenHash, Instant.now(), expiresAt);
    }

    public RefreshToken(AuthSession session, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.session = EntityValidation.requireNonNull(session, "Сессия");
        this.createdAt = EntityValidation.requireNonNull(createdAt, "Дата создания токена");
        this.tokenHash = EntityValidation.requireText(tokenHash, "Хэш токена");
        this.expiresAt = EntityValidation.requireNonNull(expiresAt, "Срок токена");
    }

    @NotNull
    public UUID getId() { return id; }

    @NotNull
    public AuthSession getSession() { return session; }

    @NotNull
    public String getTokenHash() { return tokenHash; }

    @NotNull
    public Instant getCreatedAt() { return createdAt; }

    @NotNull
    public Instant getExpiresAt() { return expiresAt; }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Nullable
    public Instant getUsedAt() { return usedAt; }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
