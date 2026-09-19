package com.team.corporate.services;

import com.team.corporate.entities.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoriesService {

    @NotNull Inventory placeProductInWarehouse(@NotNull UUID productId, @NotNull UUID warehouseId, int quantity);

    @NotNull List<Inventory> getAllByWarehouse(@NotNull UUID warehouseId);

    @NotNull List<Inventory> getAllByProduct(@NotNull UUID productId);

    @NotNull Optional<Inventory> getAllByProductAndWarehouse(@NotNull UUID productId, @NotNull UUID warehouseId);

    @NotNull Optional<Inventory> getInventory(@NotNull UUID id);

    void deleteInventory(@NotNull UUID id);

    @NotNull List<Inventory> getAll();

    @NotNull Inventory updateInventory(@NotNull UUID inventoryId, @Nullable Integer quantity);

}
