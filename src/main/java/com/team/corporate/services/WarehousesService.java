package com.team.corporate.services;

import com.team.corporate.entities.Warehouse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehousesService {
    @NotNull
    Warehouse createWarehouse(@NotNull String name, @Nullable String location);

    @NotNull
    Warehouse updateWarehouse(@NotNull UUID id, @Nullable String name, @Nullable String location);

    void deleteWarehouse(@NotNull UUID id);

    @NotNull
    List<Warehouse> getAll();

    @NotNull
    Optional<Warehouse> getWarehouse(@NotNull UUID id);
}
