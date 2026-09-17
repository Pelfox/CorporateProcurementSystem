package com.team.corporate.domain.repository;

import com.team.corporate.domain.models.Inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    Inventory add(Inventory inventory);
    List<Inventory> getAll();
    Optional<Inventory> getByProductAndWarehouseIds(UUID productId, int warehouseId);
    List<Inventory> getByProductId(UUID id);
    List<Inventory> getByWarehouseId(int id);
    Inventory update(Inventory inventory);
    void delete(UUID id);
}
