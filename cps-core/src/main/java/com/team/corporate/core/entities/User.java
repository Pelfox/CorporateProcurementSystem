package com.team.corporate.core.entities;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@SoftDelete(
        strategy = SoftDeleteType.TIMESTAMP,
        columnName = "deleted_at"
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public User() {
    }

    public User(@NotNull String username, @NotNull UserRole userRole) {
        this(username, username, userRole);
    }

    public User(@NotNull String username, @NotNull String fullName, @NotNull UserRole userRole) {
        this.username = EntityValidation.requireText(username, "Имя пользователя");
        this.fullName = EntityValidation.requireText(fullName, "Имя");
        this.userRole = EntityValidation.requireNonNull(userRole, "Роль");
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

    @NotNull
    public String getFullName() {
        return fullName;
    }

    public void setFullName(@NotNull String fullName) {
        this.fullName = EntityValidation.requireText(fullName, "Имя");
    }

    @NotNull
    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(@NotNull UserRole userRole) {
        this.userRole = EntityValidation.requireNonNull(userRole, "Роль");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Nullable
    public Instant getCreatedAt() {
        return createdAt;
    }

}
