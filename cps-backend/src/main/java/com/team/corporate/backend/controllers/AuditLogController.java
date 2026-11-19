package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.AuditLogResponse;
import com.team.corporate.backend.dto.CreateAuditLogRequest;
import com.team.corporate.backend.search.AuditLogFilter;
import com.team.corporate.backend.services.AuditLogsSearchService;
import com.team.corporate.core.services.AuditLogsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "История статусов заказов", description = "Записи о смене статуса заказов. Только ADMIN после смены временного пароля; общий журнал событий доступен через /api/admin/audit.")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
    private final AuditLogsService audit;
    private final AuditLogsSearchService search;

    public AuditLogController(AuditLogsService audit, AuditLogsSearchService search) {
        this.audit = audit;
        this.search = search;
    }

    @GetMapping
    @Operation(summary = "Получить страницу истории статусов", description = "Необязательные query-фильтры orderId, status, createdFrom и createdBefore объединяются условием И. "
            + "status фильтрует текущий статус связанного заказа. Даты — ISO-8601; createdFrom включительно, createdBefore исключительно; начало раньше конца. "
            + "page с 0, size от 1 до 100; по умолчанию 0 и 20. Сортировка только по changedAt, например sort=changedAt,asc; по умолчанию сначала новые записи. "
            + "Ответ содержит content и page с size, number, totalElements и totalPages.")
    public PagedModel<AuditLogResponse> findAll(
            @ParameterObject @Valid @ModelAttribute AuditLogFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @ParameterObject Sort sort
    ) {
        return new PagedModel<>(search.findAll(filter, page, size, sort));
    }

    @PostMapping
    @Operation(summary = "Добавить запись истории статусов", description = "Обязательны orderId, oldStatus и newStatus. Автор определяется по access JWT. "
            + "Создаёт только запись истории, не меняя статус заказа. Несуществующий заказ — 404.")
    @ApiResponse(responseCode = "201", description = "Запись истории создана")
    public ResponseEntity<AuditLogResponse> createAuditLog(
            @Valid @RequestBody CreateAuditLogRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var log = audit.createAuditLog(request.orderId(), UUID.fromString(jwt.getSubject()),
                request.oldStatus(), request.newStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuditLogResponse.of(log));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись истории статусов", description = "Выполняет мягкое удаление записи истории. Статус заказа не изменяется.")
    @ApiResponse(responseCode = "204", description = "Запись истории удалена")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuditLog(@PathVariable("id") UUID id) {
        audit.deleteAuditLog(id);
    }
}
