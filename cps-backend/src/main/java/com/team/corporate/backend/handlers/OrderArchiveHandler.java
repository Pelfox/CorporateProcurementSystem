package com.team.corporate.backend.handlers;

import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.backend.security.BusinessAccess;
import com.team.corporate.backend.http.CorrelationIdFilter;
import com.team.corporate.core.services.BulkOperationsService;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class OrderArchiveHandler {
    private final BulkOperationsService bulkOperationsService;
    private final ApiExecutor api;
    private final BusinessAccess access;
    private final HttpServletRequest request;

    public OrderArchiveHandler(@NotNull BulkOperationsService bulkOperationsService,
                               @NotNull ApiExecutor api,
                               @NotNull BusinessAccess access,
                               @NotNull HttpServletRequest request) {
        this.bulkOperationsService = bulkOperationsService;
        this.api = api;
        this.access = access;
        this.request = request;
    }

    @PostMapping("/api/orders/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable UUID id, @RequestParam UUID actingUserId) {
        String correlationId = CorrelationIdFilter.current(request);
        api.run(() -> {
            access.requireOrder(id);
            bulkOperationsService.restore(access.actor(actingUserId), id, correlationId);
        });
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/orders/{id}/permanent")
    public ResponseEntity<Void> permanentDelete(@PathVariable UUID id, @RequestParam UUID actingUserId) {
        String correlationId = CorrelationIdFilter.current(request);
        api.run(() -> {
            access.requireOrder(id);
            bulkOperationsService.permanentDelete(access.actor(actingUserId), id, correlationId);
        });
        return ResponseEntity.noContent().build();
    }
}
