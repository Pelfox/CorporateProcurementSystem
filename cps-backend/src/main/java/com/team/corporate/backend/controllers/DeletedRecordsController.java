package com.team.corporate.backend.controllers;

import com.team.corporate.backend.services.SoftDeleteAdministrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/deleted")
@Tag(name = "Удалённые записи", description = "Восстановление и окончательное удаление записей. Только ADMIN после смены временного пароля.")
@PreAuthorize("hasRole('ADMIN')")
public class DeletedRecordsController {
    private final SoftDeleteAdministrationService administration;

    public DeletedRecordsController(SoftDeleteAdministrationService administration) {
        this.administration = administration;
    }

    @PostMapping("/{entity}/{id}/restore")
    @Operation(summary = "Восстановить удалённую запись", description = "Сначала должны быть восстановлены связанные родительские записи. Для заказов и их позиций проверяется доступный остаток. "
            + "Восстановление пользователя отзывает его сессии. Уже активная запись возвращает 204, отсутствующая — 404, конфликт связей или резервов — 409.")
    @ApiResponse(responseCode = "204", description = "Запись восстановлена или уже активна")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(
            @Parameter(description = "Тип записи", schema = @Schema(allowableValues = {
                    "users", "categories", "products", "warehouses", "inventories", "orders", "order-items", "audit-logs", "tags"
            })) @PathVariable("entity") String entity,
            @PathVariable("id") UUID id
    ) {
        administration.restore(entity, id);
    }

    @DeleteMapping("/{entity}/{id}")
    @Operation(summary = "Окончательно удалить запись", description = "Безвозвратно удаляет запись и зависимые удалённые записи. Наличие активной записи в цепочке зависимостей — 409; "
            + "отсутствующая запись также возвращает 204. Для пользователя удаляются учётные данные и сессии; для товаров и тегов — связи product_tags.")
    @ApiResponse(responseCode = "204", description = "Запись окончательно удалена или уже отсутствует")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purge(
            @Parameter(description = "Тип записи", schema = @Schema(allowableValues = {
                    "users", "categories", "products", "warehouses", "inventories", "orders", "order-items", "audit-logs", "tags"
            })) @PathVariable("entity") String entity,
            @PathVariable("id") UUID id
    ) {
        administration.purge(entity, id);
    }
}
