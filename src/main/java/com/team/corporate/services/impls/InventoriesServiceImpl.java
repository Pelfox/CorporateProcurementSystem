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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InventoriesServiceImpl implements InventoriesService {

    private final ProductsRepository productsRepository;
    private final WarehousesRepository warehousesRepository;
    private final InventoriesRepository inventoriesRepository;

    public InventoriesServiceImpl(ProductsRepository productsRepository, WarehousesRepository warehousesRepository, InventoriesRepository inventoriesRepository){
       this.inventoriesRepository = inventoriesRepository;
       this.productsRepository = productsRepository;
       this.warehousesRepository = warehousesRepository;
    }
    @Override
    public @NotNull Inventory placeProductInWarehouse(@NotNull UUID productId, @NotNull UUID warehouseId, int quantity) {
        Product product = productsRepository.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным Id  не найден."));

        Warehouse warehouse = warehousesRepository.getById(warehouseId)
                .orElseThrow(()-> new WarehouseNotFoundException("Склад с указанным Id не найден."));

        Inventory inventory = new Inventory(product, warehouse, quantity);

        return inventoriesRepository.add(inventory);
    }

    @Override
    public @NotNull List<Inventory> getAllByWarehouse(@NotNull UUID warehouseId) {

        Warehouse warehouse = warehousesRepository.getById(warehouseId)
                .orElseThrow(()-> new WarehouseNotFoundException("Склад с указанным Id не найден."));

        return inventoriesRepository.getByWarehouseId(warehouseId);
    }

    @Override
    public @NotNull List<Inventory> getAllByProduct(@NotNull UUID productId) {

        Product product = productsRepository.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным Id  не найден."));

        return inventoriesRepository.getByProductId(productId);
    }



    /* Not practical */
    @Override
    public @NotNull Optional<Inventory> getAllByProductAndWarehouse(@NotNull UUID productId, @NotNull UUID warehouseId) {
        Product product = productsRepository.getById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным Id  не найден."));

        Warehouse warehouse = warehousesRepository.getById(warehouseId)
                .orElseThrow(()-> new WarehouseNotFoundException("Склад с указанным Id не найден."));

        return inventoriesRepository.getByProductAndWarehouseIds(productId, warehouseId);
    }

    @Override
    public @NotNull Optional<Inventory> getInventory(@NotNull UUID id) {
        return inventoriesRepository.getById(id);
    }

    @Override
    public void deleteInventory(@NotNull UUID id) {
        Optional<Inventory> inventory = inventoriesRepository.getById(id);
        if (inventory.isPresent()){
            inventoriesRepository.delete(id);
        }
        else {
            throw new InventoryNotFoundException("Инвентарь с указанным Id не найден.");
        }

    }

    @Override
    public @NotNull List<Inventory> getAll() {
        return inventoriesRepository.getAll();
    }

    @Override
    public @NotNull Inventory updateInventory(@NotNull UUID inventoryId, @Nullable Integer quantity) {
        Inventory inventory = inventoriesRepository.getById(inventoryId)
                .orElseThrow(()-> new InventoryNotFoundException("Инвентарь с указанным Id не найден."));

        if (quantity != null){
            inventory.setQuantity(quantity);
            return inventoriesRepository.update(inventory);
        }

        return inventory;



    }
}
