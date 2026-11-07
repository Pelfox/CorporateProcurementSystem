package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.InventoryDto;
import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.core.entities.Inventory;
import com.team.corporate.core.services.InventoriesService;
import com.team.corporate.core.services.ProductsService;
import com.team.corporate.core.services.impls.OrderStock;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class InventoryHandler {
    private final InventoriesService inventories;
    private final ProductsService products;
    private final OrderStock stock;
    private final ApiExecutor api;

    public InventoryHandler(
            @NotNull InventoriesService inventories,
            @NotNull ProductsService products,
            @NotNull OrderStock stock,
            @NotNull ApiExecutor api
    ) {
        this.inventories = inventories;
        this.products = products;
        this.stock = stock;
        this.api = api;
    }

    @GetMapping("/inventory")
    public List<InventoryDto> list(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID warehouseId
    ) {
        return api.execute(() -> {
            List<Inventory> rows;
            if (productId != null && warehouseId != null) {
                rows = inventories.getAllByProductAndWarehouse(productId, warehouseId).stream().toList();
            } else if (productId != null) {
                rows = inventories.getAllByProduct(productId);
            } else if (warehouseId != null) {
                rows = inventories.getAllByWarehouse(warehouseId);
            } else {
                rows = inventories.getAll();
            }
            return rows.stream().map(InventoryDto::from).sorted(Comparator.comparing(InventoryDto::id)).toList();
        });
    }

    @GetMapping("/inventory/{id}")
    public InventoryDto get(@PathVariable UUID id) {
        return api.execute(() -> InventoryDto.from(inventories.getInventory(id)
                .orElseThrow(() -> ApiException.notFound("Складской остаток"))));
    }

    @GetMapping("/warehouses/{warehouseId}/inventory")
    public List<InventoryDto> warehouseInventory(@PathVariable UUID warehouseId) {
        return list(null, warehouseId);
    }

    @PutMapping("/warehouses/{warehouseId}/inventory/{productId}")
    public ResponseEntity<InventoryDto> set(
            @PathVariable UUID warehouseId,
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryDto.QuantityRequest body
    ) {
        return api.execute(() -> {
            stock.lockProducts(List.of(productId));
            var existing = inventories.getAllByProductAndWarehouse(productId, warehouseId);
            var saved = existing.map(inventory -> inventories.updateInventory(inventory.getId(), body.quantity()))
                    .orElseGet(() -> inventories.placeProductInWarehouse(productId, warehouseId, body.quantity()));
            var dto = InventoryDto.from(saved);
            return existing.isPresent() ? ResponseEntity.ok(dto)
                    : ResponseEntity.created(URI.create("/api/inventory/" + saved.getId())).body(dto);
        });
    }

    @DeleteMapping("/inventory/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        api.run(() -> inventories.deleteInventory(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inventory/availability")
    public List<InventoryDto.Availability> availability(@RequestParam(required = false) UUID editingOrderId) {
        return api.execute(() -> {
            var available = stock.getAvailableQuantities(editingOrderId);
            return products.getAll().stream().map(product -> new InventoryDto.Availability(
                            product.getId(), available.getOrDefault(product.getId(), 0L)))
                    .sorted(Comparator.comparing(InventoryDto.Availability::productId)).toList();
        });
    }
}
