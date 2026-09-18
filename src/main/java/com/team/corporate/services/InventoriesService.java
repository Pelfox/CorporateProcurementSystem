package com.team.corporate.services;

import com.team.corporate.entities.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public interface InventoriesService {

    @NotNull Inventory placeProductInWarehouse(@NotNull UUID product_id, @NotNull UUID warehouse_id, int quantity);

    @NotNull List<Inventory> getAllByWarehouse(@NotNull UUID warehouse_id);

    @NotNull List<Inventory> getAllByProduct(@NotNull UUID product_id);

    @NotNull List<Inventory> getAllByProductAndWarehouse(@NotNull UUID product_id, @NotNull UUID warehouse_id);

    @NotNull Inventory getInventory(@NotNull UUID id);

    void deleteInventory(@NotNull UUID id);

    @NotNull Inventory getAll();

    boolean checkIfExists(@NotNull UUID id);

}
