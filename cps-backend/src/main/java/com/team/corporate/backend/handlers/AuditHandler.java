package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.AuditPageDto;
import com.team.corporate.backend.dto.AuditQueryDto;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.core.services.AuditLogsService;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditHandler {
    private final AuditLogsService auditLogsService;
    private final ApiExecutor api;

    public AuditHandler(@NotNull AuditLogsService auditLogsService, @NotNull ApiExecutor api) {
        this.auditLogsService = auditLogsService;
        this.api = api;
    }

    @GetMapping
    public AuditPageDto list(@Valid @ModelAttribute AuditQueryDto query) {
        return api.execute(() -> {
            var serviceQuery = query.toServiceQuery();
            var page = auditLogsService.search(serviceQuery);
            return AuditPageDto.from(page, serviceQuery.page(), serviceQuery.size());
        });
    }
}