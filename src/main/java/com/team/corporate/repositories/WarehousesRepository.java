package com.team.corporate.repositories;

import com.team.corporate.models.Warehouse;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface WarehousesRepository {
    @NotNull Warehouse add(@NotNull Warehouse warehouse);

    @NotNull List<Warehouse> getAll();

    Optional<Warehouse> getById(int id);

    @NotNull Warehouse update(@NotNull Warehouse warehouse);

    void delete(int id);
}
