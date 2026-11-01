package com.team.corporate.core.repositories;

import com.team.corporate.core.entities.Warehouse;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehousesRepository {
    @NotNull
    Warehouse add(@NotNull Warehouse warehouse);

    @NotNull
    List<Warehouse> getAll();

    @NotNull
    Optional<Warehouse> getById(@NotNull UUID id);

    @NotNull
    Warehouse update(@NotNull Warehouse warehouse);

    void delete(@NotNull UUID id);
}
