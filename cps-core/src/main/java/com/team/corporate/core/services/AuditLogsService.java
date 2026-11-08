package com.team.corporate.core.services;

import com.team.corporate.core.entities.AuditEventType;
import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.entities.EntityType;
import com.team.corporate.core.entities.OrderStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogsService {
    @NotNull
    AuditLog record(@NotNull EntityType entityType, @NotNull UUID entityUuid,
                    @NotNull AuditEventType eventType, @NotNull UUID changedByUserId,
                    @NotNull String description, @Nullable String correlationId);

    @NotNull
    AuditLog createAuditLog(@NotNull UUID orderId, @NotNull UUID changedByUserId,
                            @NotNull OrderStatus oldStatus, @NotNull OrderStatus newStatus);

    @NotNull Optional<AuditLog> getAuditLog(@NotNull UUID id);
    @NotNull List<AuditLog> getAllByOrder(@NotNull UUID orderId);
    @NotNull List<AuditLog> getAllByChangedBy(@NotNull UUID changedByUserId);
    @NotNull List<AuditLog> getAllByOldStatus(@NotNull OrderStatus status);
    @NotNull List<AuditLog> getAllByNewStatus(@NotNull OrderStatus status);

    @NotNull AuditPage search(@NotNull AuditQuery query);

    record AuditPage(@NotNull List<AuditLog> items, long total) {}

    record AuditQuery(
            @Nullable EntityType entityType, @Nullable UUID entityUuid,
            @Nullable AuditEventType eventType, @Nullable UUID changedByUserId,
            @Nullable String textQuery, @Nullable Instant dateFrom, @Nullable Instant dateTo,
            @Nullable String correlationId, @NotNull String sortField, boolean sortDesc,
            int page, int size
    ) {
        public AuditQuery {
            sortField = sortField == null || sortField.isBlank() ? "changedAt" : sortField.strip();
            if (size < 1) size = 20;
            if (size > 200) size = 200;
            if (page < 0) page = 0;
        }
        public int offset() { return page * size; }
    }
}