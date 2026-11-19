package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateInventoryRequest;
import com.team.corporate.backend.dto.InventoryResponse;
import com.team.corporate.backend.dto.UpdateInventoryRequest;
import com.team.corporate.backend.search.InventoryFilter;
import com.team.corporate.backend.services.InventoriesSearchService;
import com.team.corporate.core.exceptions.InventoryNotFoundException;
import com.team.corporate.core.services.InventoriesService;
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
@RequestMapping("/api/inventories")
@Tag(name = "Складские остатки", description = "Остатки товаров на складах. Чтение: USER, MANAGER и ADMIN; изменение: MANAGER и ADMIN после смены временного пароля.")
public class InventoriesController {
    private final InventoriesService inventories;
    private final InventoriesSearchService search;

    public InventoriesController(InventoriesService inventories, InventoriesSearchService search) {
        this.inventories = inventories;
        this.search = search;
    }

    @GetMapping
    @Operation(summary = "Получить страницу складских остатков", description = "Необязательные query-фильтры productId, warehouseId, productName, warehouseName, minQuantity и maxQuantity объединяются условием И. "
            + "Имена ищутся по подстроке без учёта регистра; границы количества включительны и неотрицательны, minQuantity ≤ maxQuantity. "
            + "page с 0, size от 1 до 100; по умолчанию 0 и 20. sort: id, quantity, product.name, product.sku, warehouse.name; пример sort=quantity,desc. "
            + "Ответ содержит content и page с size, number, totalElements и totalPages.")
    public PagedModel<InventoryResponse> getInventories(
            @ParameterObject @Valid @ModelAttribute InventoryFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @ParameterObject Sort sort
    ) {
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить складской остаток по UUID", description = "Отсутствующая или удалённая запись — 404.")
    public InventoryResponse getInventory(@PathVariable("id") UUID id) {
        return inventories.getInventory(id)
                .map(InventoryResponse::of)
                .orElseThrow(() -> new InventoryNotFoundException("Складской остаток не найден."));
    }

    @PostMapping
    @Operation(summary = "Разместить товар на складе", description = "Только MANAGER и ADMIN. Обязательны productId, warehouseId и неотрицательное quantity. "
            + "Для пары товар/склад допускается одна запись; для изменения количества используйте PATCH. Ответ содержит запись и заголовок Location.")
    @ApiResponse(responseCode = "201", description = "Складской остаток создан")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        var inventory = inventories.placeProductInWarehouse(
                request.productId(), request.warehouseId(), request.quantity());
        return ResponseEntity.created(URI.create("/api/inventories/" + inventory.getId()))
                .body(InventoryResponse.of(inventory));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменить количество товара на складе", description = "Только MANAGER и ADMIN. quantity задаёт новое абсолютное количество, а не прибавку. "
            + "Количество неотрицательно; суммарный остаток нельзя уменьшить ниже зарезервированного.")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public InventoryResponse updateInventory(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateInventoryRequest request
    ) {
        return InventoryResponse.of(inventories.updateInventory(id, request.quantity()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить складской остаток", description = "Только MANAGER и ADMIN. Выполняет мягкое удаление; операция запрещена, если оставшихся остатков недостаточно для резервов заказов.")
    @ApiResponse(responseCode = "204", description = "Складской остаток удалён")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventory(@PathVariable("id") UUID id) {
        inventories.deleteInventory(id);
    }
}
