package com.team.corporate.services.impls;

import com.team.corporate.entities.Warehouse;
import com.team.corporate.repositories.WarehousesRepository;
import com.team.corporate.services.WarehousesService;
import jakarta.persistence.EntityNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WarehousesServiceImpl implements WarehousesService
{
    private final WarehousesRepository warehousesRepository;

    public WarehousesServiceImpl(WarehousesRepository warehousesRepository) {
        this.warehousesRepository = warehousesRepository;
    }

    @Override
    public @NotNull Warehouse createWarehouse(@NotNull String name, @Nullable String location) {
        Warehouse newWarehouse = new Warehouse(name, location);
        return warehousesRepository.add(newWarehouse);
    }

    @Override
    public @NotNull Warehouse updateWarehouse(@NotNull UUID id, @Nullable String name, @Nullable String location) {
        Warehouse warehouse = warehousesRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Склад с указанным id не найден"));

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
    public @NotNull List<Warehouse> getAll() {
        return warehousesRepository.getAll();
    }

    @Override
    public @NotNull Optional<Warehouse> getWarehouse(@NotNull UUID id) {
        return warehousesRepository.getById(id);
    }
}
