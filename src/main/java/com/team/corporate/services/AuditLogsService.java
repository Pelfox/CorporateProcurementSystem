package com.team.corporate.services;

import com.team.corporate.entities.AuditLog;
import com.team.corporate.entities.OrderStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogsService {
    @NotNull
    AuditLog createAuditLog(@NotNull UUID orderId, @NotNull UUID changedByUserId, @NotNull OrderStatus oldStatus, @NotNull OrderStatus newStatus);

    @NotNull
    Optional<AuditLog> getAuditLog(@NotNull UUID id);

    @NotNull
    List<AuditLog> getAllByOrder(@NotNull UUID orderId);

    @NotNull
    List<AuditLog> getAllByChangedBy(@NotNull UUID changedByUserId);

    @NotNull
    List<AuditLog> getAllByOldStatus(@NotNull OrderStatus status);

    @NotNull
    List<AuditLog> getAllByNewStatus(@NotNull OrderStatus status);
}
