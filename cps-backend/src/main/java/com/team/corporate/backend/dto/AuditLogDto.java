package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.AuditEventType;
import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.entities.EntityType;
import com.team.corporate.core.entities.OrderStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        EntityType entityType,
        UUID entityUuid,
        AuditEventType eventType,
        String description,
        String correlationId,
        UUID changedBy,
        String changedByUsername,
        Instant changedAt,
        UUID orderId,
        OrderStatus oldStatus,
        OrderStatus newStatus
) {
    public static AuditLogDto from(@NotNull AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getEntityType(),
                log.getEntityUuid(),
                log.getEventType(),
                log.getDescription(),
                log.getCorrelationId(),
                log.getChangedBy().getId(),
                log.getChangedBy().getUsername(),
                log.getChangedAt(),
                log.getOrder() == null ? null : log.getOrder().getId(),
                log.getOldStatus(),
                log.getNewStatus()
        );
    }
}