package com.team.corporate.services.impls;

import com.team.corporate.entities.Inventory;
import com.team.corporate.entities.Product;
import com.team.corporate.entities.Warehouse;
import com.team.corporate.exceptions.InventoryNotFoundException;
import com.team.corporate.exceptions.ProductNotFoundException;
import com.team.corporate.exceptions.WarehouseNotFoundException;
import com.team.corporate.repositories.InventoriesRepository;
import com.team.corporate.repositories.ProductsRepository;
import com.team.corporate.repositories.WarehousesRepository;
import com.team.corporate.services.InventoriesService;
import com.team.corporate.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InventoriesServiceImpl implements InventoriesService {
    private final ProductsRepository productsRepository;
    private final WarehousesRepository warehousesRepository;
    private final InventoriesRepository inventoriesRepository;

    public InventoriesServiceImpl(@NotNull ProductsRepository productsRepository,
                                  @NotNull WarehousesRepository warehousesRepository,
                                  @NotNull InventoriesRepository inventoriesRepository) {
        this.inventoriesRepository = inventoriesRepository;
        this.productsRepository = productsRepository;
        this.warehousesRepository = warehousesRepository;
    }

    @Override
    @NotNull
    public Inventory placeProductInWarehouse(@NotNull UUID productId,
                                             @NotNull UUID warehouseId,
                                             int quantity) {
        EntityValidation.requireNonNull(productId, "Идентификатор товара");
        EntityValidation.requireNonNull(warehouseId, "Идентификатор склада");
        EntityValidation.requireAtLeast(quantity, 0, "Остаток");
        if (inventoriesRepository.getByProductAndWarehouseIds(productId, warehouseId).isPresent()) {
            throw new IllegalArgumentException("Товар уже размещён на этом складе. Измените существующий остаток.");
        }
        Product product = productsRepository.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным идентификатором не найден."));
        Warehouse warehouse = warehousesRepository.getById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Склад с указанным идентификатором не найден."));
        Inventory inventory = new Inventory(product, warehouse, quantity);
        return inventoriesRepository.add(inventory);
    }

    @Override
    @NotNull
    public List<Inventory> getAllByWarehouse(@NotNull UUID warehouseId) {
        EntityValidation.requireNonNull(warehouseId, "Идентификатор склада");
        warehousesRepository.getById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Склад с указанным идентификатором не найден."));
        return inventoriesRepository.getByWarehouseId(warehouseId);
    }

    @Override
    @NotNull
    public List<Inventory> getAllByProduct(@NotNull UUID productId) {
        EntityValidation.requireNonNull(productId, "Идентификатор товара");
        productsRepository.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным идентификатором не найден."));
        return inventoriesRepository.getByProductId(productId);
    }

    @Override
    @NotNull
    public Optional<Inventory> getAllByProductAndWarehouse(@NotNull UUID productId, @NotNull UUID warehouseId) {
        EntityValidation.requireNonNull(productId, "Идентификатор товара");
        EntityValidation.requireNonNull(warehouseId, "Идентификатор склада");
        productsRepository.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным идентификатором не найден."));
        warehousesRepository.getById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Склад с указанным идентификатором не найден."));
        return inventoriesRepository.getByProductAndWarehouseIds(productId, warehouseId);
    }

    @Override
    @NotNull
    public Optional<Inventory> getInventory(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return inventoriesRepository.getById(id);
    }

    @Override
    public void deleteInventory(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        Optional<Inventory> inventory = inventoriesRepository.getById(id);
        if (inventory.isPresent()) {
            inventoriesRepository.delete(id);
        } else {
            throw new InventoryNotFoundException("Инвентарь с указанным идентификатором не найден.");
        }
    }

    @Override
    @NotNull
    public List<Inventory> getAll() {
        return inventoriesRepository.getAll();
    }

    @Override
    @NotNull
    public Inventory updateInventory(@NotNull UUID inventoryId, int quantity) {
        EntityValidation.requireNonNull(inventoryId, "Идентификатор складского остатка");
        Inventory inventory = inventoriesRepository.getById(inventoryId)
                .orElseThrow(() -> new InventoryNotFoundException("Инвентарь с указанным идентификатором не найден."));
        inventory.setQuantity(quantity);
        return inventoriesRepository.update(inventory);
    }
}
