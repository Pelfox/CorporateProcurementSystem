package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.BulkOperationDto;
import com.team.corporate.backend.dto.BulkOperationResultDto;
import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.http.ApiExecutor;
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
    private final ApiExecutor api;
    private final HttpServletRequest request;

    public BulkOperationsHandler(@NotNull BulkOperationsService bulkOperationsService,
                                 @NotNull ApiExecutor api,
                                 @NotNull HttpServletRequest request) {
        this.bulkOperationsService = bulkOperationsService;
        this.api = api;
        this.request = request;
    }

    @PostMapping
    public ResponseEntity<BulkOperationResultDto> execute(
            @RequestParam UUID actingUserId,
            @Valid @RequestBody BulkOperationDto body) {
        String correlationId = CorrelationIdFilter.current(request);
        var result = api.execute(() -> switch (body.action()) {
            case ARCHIVE -> bulkOperationsService.bulkArchive(actingUserId, body.orderIds(), correlationId);
            case RESTORE -> bulkOperationsService.bulkRestore(actingUserId, body.orderIds(), correlationId);
            case CHANGE_STATUS -> {
                if (body.status() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Для CHANGE_STATUS укажите поле status.");
                yield bulkOperationsService.bulkChangeStatus(actingUserId, body.orderIds(), body.status(), correlationId);
            }
            case ADD_TAG -> {
                if (body.tagName() == null || body.tagName().isBlank())
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Для ADD_TAG укажите поле tagName.");
                yield bulkOperationsService.bulkAddTag(actingUserId, body.orderIds(), body.tagName(), correlationId);
            }
            case REMOVE_TAG -> {
                if (body.tagName() == null || body.tagName().isBlank())
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Для REMOVE_TAG укажите поле tagName.");
                yield bulkOperationsService.bulkRemoveTag(actingUserId, body.orderIds(), body.tagName(), correlationId);
            }
        });
        return ResponseEntity.ok(BulkOperationResultDto.from(result, body.orderIds().size()));
    }
}