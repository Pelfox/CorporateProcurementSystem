package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.entities.OrderStatus;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID orderId,
        UUID changedBy,
        String changedByUsername,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        Instant changedAt
) {
    public static AuditLogDto from(@NotNull AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getOrder().getId(),
                log.getChangedBy().getId(),
                log.getChangedBy().getUsername(),
                log.getOldStatus(),
                log.getNewStatus(),
                log.getChangedAt()
        );
    }
}
