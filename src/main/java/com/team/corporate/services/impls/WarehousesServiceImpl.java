package com.team.corporate.services.impls;

import com.team.corporate.entities.Warehouse;
import com.team.corporate.exceptions.WarehouseNotFoundException;
import com.team.corporate.repositories.WarehousesRepository;
import com.team.corporate.services.WarehousesService;
import com.team.corporate.utils.EntityValidation;
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
        EntityValidation.requireNonNull(name, "Название");
        Warehouse newWarehouse = new Warehouse(name, location);
        return warehousesRepository.add(newWarehouse);
    }

    @Override
    @NotNull
    public Warehouse updateWarehouse(@NotNull UUID id, @Nullable String name, @Nullable String location) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        Warehouse warehouse = warehousesRepository.getById(id)
                .orElseThrow(() -> new WarehouseNotFoundException("Склад с указанным идентификатором не найден"));
        if (name != null) {
            EntityValidation.requireText(name, "Название склада");
        }
        EntityValidation.optionalText(location, "Адрес");
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
        EntityValidation.requireNonNull(id, "Идентификатор");
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
        EntityValidation.requireNonNull(id, "Идентификатор");
        return warehousesRepository.getById(id);
    }
}
