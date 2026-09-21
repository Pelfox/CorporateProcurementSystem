package com.team.corporate.services.impls;

import com.team.corporate.entities.Warehouse;
import com.team.corporate.exceptions.WarehouseNotFoundException;
import com.team.corporate.repositories.WarehousesRepository;
import com.team.corporate.services.WarehousesService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WarehousesServiceImpl implements WarehousesService {
    private final WarehousesRepository warehousesRepository;

    public WarehousesServiceImpl(@NotNull WarehousesRepository warehousesRepository) {
        this.warehousesRepository = warehousesRepository;
    }

    @Override
    @NotNull
    public Warehouse createWarehouse(@NotNull String name, @Nullable String location) {
        Warehouse newWarehouse = new Warehouse(name, location);
        return warehousesRepository.add(newWarehouse);
    }

    @Override
    @NotNull
    public Warehouse updateWarehouse(@NotNull UUID id, @Nullable String name, @Nullable String location) {
        Warehouse warehouse = warehousesRepository.getById(id)
                .orElseThrow(() -> new WarehouseNotFoundException("Склад с указанным ID не найден"));
        if (name != null) {
            warehouse.setName(name);
        }
        if (location != null) {
            warehouse.setLocation(location);
        }
        return warehousesRepository.update(warehouse);
    }

    @Override
    public void deleteWarehouse(@NotNull UUID id) {
        warehousesRepository.delete(id);
    }

    @Override
    @NotNull
    public List<Warehouse> getAll() {
        return warehousesRepository.getAll();
    }

    @Override
    @NotNull
    public Optional<Warehouse> getWarehouse(@NotNull UUID id) {
        return warehousesRepository.getById(id);
    }
}
