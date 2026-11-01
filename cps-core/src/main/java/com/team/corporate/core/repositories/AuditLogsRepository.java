package com.team.corporate.core.repositories;

import com.team.corporate.core.entities.AuditLog;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogsRepository {
    @NotNull
    AuditLog add(@NotNull AuditLog auditLog);

    @NotNull
    List<AuditLog> getAll();

    @NotNull
    Optional<AuditLog> getById(@NotNull UUID id);

    @NotNull
    List<AuditLog> getByOrderId(@NotNull UUID id);

    @NotNull
    List<AuditLog> getByUserId(@NotNull UUID id);

    void delete(@NotNull UUID id);
}
