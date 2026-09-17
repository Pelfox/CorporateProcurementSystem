package com.team.corporate.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false)
    private OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private OrderStatus newStatus;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    public AuditLog() {
    }

    public AuditLog(@NotNull Order order,
                    @NotNull User changedBy,
                    @NotNull OrderStatus oldStatus,
                    @NotNull OrderStatus newStatus) {
        this.order = order;
        this.changedBy = changedBy;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public Order getOrder() {
        return order;
    }

    public void setOrder(@NotNull Order order) {
        this.order = order;
    }

    @NotNull
    public User getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(@NotNull User changedBy) {
        this.changedBy = changedBy;
    }

    @NotNull
    public OrderStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(@NotNull OrderStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    @NotNull
    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(@NotNull OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    @NotNull
    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
