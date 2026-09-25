package com.team.corporate.entities;

import com.team.corporate.utils.EntityValidation;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus status;

    @Column(name = "notes", length = EntityValidation.TEXT_MAX_LENGTH)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Order() {
    }

    public Order(@NotNull User user, @NotNull OrderStatus status, @Nullable String notes) {
        this.user = EntityValidation.requireNonNull(user, "Пользователь");
        this.status = EntityValidation.requireNonNull(status, "Статус");
        this.notes = EntityValidation.optionalText(notes, "Примечание");
    }

    public Order(@NotNull User user, @NotNull OrderStatus status) {
        this.user = EntityValidation.requireNonNull(user, "Пользователь");
        this.status = EntityValidation.requireNonNull(status, "Статус");
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public User getUser() {
        return user;
    }

    public void setUser(@NotNull User user) {
        this.user = EntityValidation.requireNonNull(user, "Пользователь");
    }

    @NotNull
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(@NotNull OrderStatus status) {
        this.status = EntityValidation.requireNonNull(status, "Статус");
    }

    @Nullable
    public String getNotes() {
        return notes;
    }

    public void setNotes(@Nullable String notes) {
        this.notes = EntityValidation.optionalText(notes, "Примечание");
    }

    @Nullable
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
