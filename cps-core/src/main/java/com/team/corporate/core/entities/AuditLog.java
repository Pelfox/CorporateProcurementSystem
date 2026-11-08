package com.team.corporate.core.entities;

import com.team.corporate.core.utils.EntityValidation;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 50)
    private EntityType entityType;

    @Column(name = "entity_uuid")
    private UUID entityUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50)
    private AuditEventType eventType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    // Общие поля

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    // Старые поля (теперь nullable для обратной совместимости)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 50)
    private OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 50)
    private OrderStatus newStatus;

    public AuditLog() {
    }

    public AuditLog(@NotNull EntityType entityType,
                    @NotNull UUID entityUuid,
                    @NotNull AuditEventType eventType,
                    @NotNull User changedBy,
                    @NotNull String description,
                    @Nullable String correlationId) {
        this.entityType = EntityValidation.requireNonNull(entityType, "Тип сущности");
        this.entityUuid = EntityValidation.requireNonNull(entityUuid, "UUID сущности");
        this.eventType = EntityValidation.requireNonNull(eventType, "Тип события");
        this.changedBy = EntityValidation.requireNonNull(changedBy, "Автор изменения");
        this.description = EntityValidation.requireText(description, "Описание события аудита");
        this.correlationId = correlationId;
    }

    public AuditLog(@NotNull Order order,
                    @NotNull User changedBy,
                    @NotNull OrderStatus oldStatus,
                    @NotNull OrderStatus newStatus,
                    @Nullable String correlationId) {
        this.order = EntityValidation.requireNonNull(order, "Заказ");
        this.changedBy = EntityValidation.requireNonNull(changedBy, "Автор изменения");
        this.oldStatus = EntityValidation.requireNonNull(oldStatus, "Предыдущий статус");
        this.newStatus = EntityValidation.requireNonNull(newStatus, "Новый статус");
        this.entityType = EntityType.ORDER;
        this.entityUuid = order.getId();
        this.eventType = AuditEventType.STATUS_CHANGED;
        this.description = "Статус заказа изменён: " + oldStatus + " → " + newStatus;
        this.correlationId = correlationId;
    }

    public AuditLog(@NotNull Order order,
                    @NotNull User changedBy,
                    @NotNull OrderStatus oldStatus,
                    @NotNull OrderStatus newStatus) {
        this(order, changedBy, oldStatus, newStatus, null);
    }

    @NotNull
    public UUID getId() { return id; }

    @Nullable
    public EntityType getEntityType() { return entityType; }
    public void setEntityType(@NotNull EntityType entityType) { this.entityType = EntityValidation.requireNonNull(entityType, "Тип сущности"); }

    @Nullable
    public UUID getEntityUuid() { return entityUuid; }
    public void setEntityUuid(@NotNull UUID entityUuid) { this.entityUuid = EntityValidation.requireNonNull(entityUuid, "UUID сущности"); }

    @Nullable
    public AuditEventType getEventType() { return eventType; }
    public void setEventType(@NotNull AuditEventType eventType) { this.eventType = EntityValidation.requireNonNull(eventType, "Тип события"); }

    @Nullable
    public String getDescription() { return description; }
    public void setDescription(@NotNull String description) { this.description = EntityValidation.requireText(description, "Описание события аудита"); }

    @Nullable
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(@Nullable String correlationId) { this.correlationId = correlationId; }

    @NotNull
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(@NotNull User changedBy) { this.changedBy = EntityValidation.requireNonNull(changedBy, "Автор изменения"); }

    @Nullable
    public Instant getChangedAt() { return changedAt; }

    @Nullable
    public Order getOrder() { return order; }
    public void setOrder(@Nullable Order order) { this.order = order; }

    @Nullable
    public OrderStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(@Nullable OrderStatus oldStatus) { this.oldStatus = oldStatus; }

    @Nullable
    public OrderStatus getNewStatus() { return newStatus; }
    public void setNewStatus(@Nullable OrderStatus newStatus) { this.newStatus = newStatus; }
}