package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.AuditEventType;
import com.team.corporate.core.entities.EntityType;
import com.team.corporate.core.services.AuditLogsService.AuditQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record AuditQueryDto(
        @Nullable EntityType entityType,
        @Nullable UUID entityUuid,
        @Nullable AuditEventType eventType,
        @Nullable UUID changedByUserId,
        @Nullable String q,
        @Nullable Instant dateFrom,
        @Nullable Instant dateTo,
        @Nullable String correlationId,
        @Nullable String sort,
        @Nullable Boolean desc,
        @Min(0) Integer page,
        @Min(1) @Max(200) Integer size
) {
    public AuditQueryDto {
        if (page == null) page = 0;
        if (size == null) size = 20;
    }

    public AuditQuery toServiceQuery() {
        return new AuditQuery(entityType, entityUuid, eventType, changedByUserId, q,
                dateFrom, dateTo, correlationId,
                sort == null || sort.isBlank() ? "changedAt" : sort,
                desc != null && desc, page, size);
    }
}