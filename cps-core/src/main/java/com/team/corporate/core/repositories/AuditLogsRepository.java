package com.team.corporate.core.repositories;

import com.team.corporate.core.entities.AuditEventType;
import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.entities.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogsRepository {
    @NotNull AuditLog add(@NotNull AuditLog auditLog);
    @NotNull Optional<AuditLog> getById(@NotNull UUID id);
    void delete(@NotNull UUID id);

    // Старые методы
    @NotNull List<AuditLog> getAll();
    @NotNull List<AuditLog> getByOrderId(@NotNull UUID id);
    @NotNull List<AuditLog> getByUserId(@NotNull UUID id);

    // Новые методы поиска с пагинацией
    @NotNull
    List<AuditLog> search(@NotNull AuditFilter filter, @NotNull String sortField,
                          boolean sortDesc, int offset, int limit);
    long count(@NotNull AuditFilter filter);

    record AuditFilter(
            @Nullable EntityType entityType,
            @Nullable UUID entityUuid,
            @Nullable AuditEventType eventType,
            @Nullable UUID changedByUserId,
            @Nullable String textQuery,
            @Nullable Instant dateFrom,
            @Nullable Instant dateTo,
            @Nullable String correlationId
    ) {
        public AuditFilter {
            textQuery = textQuery == null || textQuery.isBlank() ? null : textQuery.strip().toLowerCase();
            correlationId = correlationId == null || correlationId.isBlank() ? null : correlationId.strip();
        }
    }
}