package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.BulkOperationDto;
import com.team.corporate.backend.dto.BulkOperationResultDto;
import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.security.BusinessAccess;
import org.springframework.security.access.AccessDeniedException;
import com.team.corporate.backend.http.CorrelationIdFilter;
import com.team.corporate.core.services.BulkOperationsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/bulk")
public class BulkOperationsHandler {
    private final BulkOperationsService bulkOperationsService;
    private final BusinessAccess access;
    private final HttpServletRequest request;

    public BulkOperationsHandler(@NotNull BulkOperationsService bulkOperationsService,
                                 @NotNull BusinessAccess access,
                                 @NotNull HttpServletRequest request) {
        this.bulkOperationsService = bulkOperationsService;
        this.access = access;
        this.request = request;
    }

    @PostMapping
    public ResponseEntity<BulkOperationResultDto> execute(
            @RequestParam UUID actingUserId,
            @Valid @RequestBody BulkOperationDto body) {
        UUID actor = access.actor(actingUserId);
        if (body.action() == BulkOperationDto.BulkAction.CHANGE_STATUS && !access.isManager()) {
            throw new AccessDeniedException("Требуется роль менеджера.");
        }
        for (UUID id : body.orderIds()) {
            try {
                access.requireOrder(id);
            } catch (ApiException error) {
                // Missing orders are reported per item by the bulk service.
                if (error.status() != HttpStatus.NOT_FOUND) throw error;
            }
        }
        String correlationId = CorrelationIdFilter.current(request);
        // Each item owns its transaction so one failure cannot roll back successful items.
        var result = switch (body.action()) {
            case ARCHIVE -> bulkOperationsService.bulkArchive(actor, body.orderIds(), correlationId);
            case RESTORE -> bulkOperationsService.bulkRestore(actor, body.orderIds(), correlationId);
            case CHANGE_STATUS -> {
                if (body.status() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Для CHANGE_STATUS укажите поле status.");
                yield bulkOperationsService.bulkChangeStatus(actor, body.orderIds(), body.status(), correlationId);
            }
            case ADD_TAG -> {
                if (body.tagName() == null || body.tagName().isBlank())
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Для ADD_TAG укажите поле tagName.");
                yield bulkOperationsService.bulkAddTag(actor, body.orderIds(), body.tagName(), correlationId);
            }
            case REMOVE_TAG -> {
                if (body.tagName() == null || body.tagName().isBlank())
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Для REMOVE_TAG укажите поле tagName.");
                yield bulkOperationsService.bulkRemoveTag(actor, body.orderIds(), body.tagName(), correlationId);
            }
        };
        return ResponseEntity.ok(BulkOperationResultDto.from(result, body.orderIds().size()));
    }
}
