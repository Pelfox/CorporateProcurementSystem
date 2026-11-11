package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.Inventory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryDto(
        UUID id,
        UUID productId,
        String productName,
        String sku,
        UUID warehouseId,
        String warehouseName,
        int quantity
) {
    public static InventoryDto from(@NotNull Inventory inventory) {
        return new InventoryDto(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getProduct().getName(),
                inventory.getProduct().getSku(),
                inventory.getWarehouse().getId(),
                inventory.getWarehouse().getName(),
                inventory.getQuantity()
        );
    }

    public record QuantityRequest(@NotNull @Min(0) Integer quantity) {
    }

    public record Availability(@NotNull UUID productId, long availableQuantity) {
    }
}
