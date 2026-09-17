package com.team.corporate.domain.repository;

import com.team.corporate.domain.models.AuditLog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {
    AuditLog add(AuditLog auditLog);
    List<AuditLog> getAll();
    Optional<AuditLog> getById(UUID id);
    List<AuditLog> getByOrderId(UUID id);
    List<AuditLog> getByUserId(UUID id);
}
