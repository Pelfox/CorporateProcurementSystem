package com.team.corporate.core.repositories;

import com.team.corporate.core.entities.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoriesRepository {
    @NotNull
    Inventory add(@NotNull Inventory inventory);

    @NotNull
    List<Inventory> getAll();

    @NotNull
    Optional<Inventory> getById(@NotNull UUID id);

    @NotNull
    Optional<Inventory> getByIdForUpdate(@NotNull UUID id);

    @NotNull
    Optional<Inventory> getByProductAndWarehouseIds(@NotNull UUID productId, @NotNull UUID warehouseId);

    @NotNull
    List<Inventory> getByProductId(@NotNull UUID id);

    @NotNull
    List<Inventory> getByWarehouseId(@NotNull UUID id);

    @NotNull
    Inventory update(@NotNull Inventory inventory);

    void delete(@NotNull UUID id);
}
