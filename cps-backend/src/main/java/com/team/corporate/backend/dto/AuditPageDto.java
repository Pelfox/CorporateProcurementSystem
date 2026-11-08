package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.services.AuditLogsService.AuditPage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AuditPageDto(
        List<AuditLogDto> items, long total, int page, int size, int totalPages
) {
    public static AuditPageDto from(@NotNull AuditPage page, int requestedPage, int requestedSize) {
        List<AuditLogDto> items = page.items().stream().map(AuditLogDto::from).toList();
        int totalPages = requestedSize == 0 ? 0 : (int) Math.ceil((double) page.total() / requestedSize);
        return new AuditPageDto(items, page.total(), requestedPage, requestedSize, totalPages);
    }
}