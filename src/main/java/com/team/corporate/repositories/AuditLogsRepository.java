package com.team.corporate.repositories;

import com.team.corporate.models.AuditLog;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogsRepository {
    AuditLog add(@NotNull AuditLog auditLog);

    @NotNull List<AuditLog> getAll();

    Optional<AuditLog> getById(@NotNull UUID id);

    @NotNull List<AuditLog> getByOrderId(@NotNull UUID id);

    @NotNull List<AuditLog> getByUserId(@NotNull UUID id);
}
