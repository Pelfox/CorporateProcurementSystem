package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateWarehouseRequest;
import com.team.corporate.backend.dto.UpdateWarehouseRequest;
import com.team.corporate.backend.dto.WarehouseResponse;
import com.team.corporate.backend.search.WarehouseFilter;
import com.team.corporate.backend.services.WarehousesSearchService;
import com.team.corporate.core.exceptions.WarehouseNotFoundException;
import com.team.corporate.core.services.WarehousesService;
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
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouses")
@Tag(name = "Склады", description = "Справочник складов. Чтение: USER, MANAGER и ADMIN; изменение: MANAGER и ADMIN после смены временного пароля.")
public class WarehouseController {
    private final WarehousesService warehouses;
    private final WarehousesSearchService search;

    public WarehouseController(WarehousesService warehouses, WarehousesSearchService search) {
        this.warehouses = warehouses;
        this.search = search;
    }

    @GetMapping
    @Operation(summary = "Получить страницу складов", description = "Необязательные query-фильтры name и location ищут подстроки без учёта регистра и объединяются условием И. "
            + "page с 0, size от 1 до 100; по умолчанию 0 и 20. sort: id, name, location; пример sort=name,asc. "
            + "Ответ содержит content и page с size, number, totalElements и totalPages.")
    public PagedModel<WarehouseResponse> getWarehouses(
            @ParameterObject @Valid @ModelAttribute WarehouseFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @ParameterObject Sort sort
    ) {
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить склад по UUID", description = "Отсутствующий или удалённый склад — 404.")
    public WarehouseResponse getWarehouse(@PathVariable("id") UUID id) {
        return warehouses.getWarehouse(id)
                .map(WarehouseResponse::of)
                .orElseThrow(() -> new WarehouseNotFoundException("Склад не найден."));
    }

    @PostMapping
    @Operation(summary = "Создать склад", description = "Только MANAGER и ADMIN. Обязателен name; location необязателен. Ответ содержит созданный склад и заголовок Location.")
    @ApiResponse(responseCode = "201", description = "Склад создан")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request
    ) {
        var warehouse = warehouses.createWarehouse(request.name(), request.location());
        return ResponseEntity.created(URI.create("/api/warehouses/" + warehouse.getId()))
                .body(WarehouseResponse.of(warehouse));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменить склад", description = "Только MANAGER и ADMIN. Обновляет переданные name и location; null сохраняет прежнее значение. Отсутствующий или удалённый склад — 404.")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public WarehouseResponse updateWarehouse(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateWarehouseRequest request
    ) {
        return WarehouseResponse.of(warehouses.updateWarehouse(id, request.name(), request.location()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить склад", description = "Только MANAGER и ADMIN. Выполняет мягкое удаление склада; восстановление и окончательное удаление доступны ADMIN через /api/admin/deleted.")
    @ApiResponse(responseCode = "204", description = "Склад удалён")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWarehouse(@PathVariable("id") UUID id) {
        warehouses.deleteWarehouse(id);
    }
}
