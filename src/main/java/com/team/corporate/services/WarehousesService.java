package com.team.corporate.services;

import com.team.corporate.entities.User;
import com.team.corporate.entities.Warehouse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface WarehousesService {

    @NotNull Warehouse createWarehouse(@NotNull String name, @Nullable String location);

    @NotNull Warehouse updateWarehouse(String name, @Nullable String location);

    boolean checkIfExists(@NotNull UUID id);

    void deleteWarehouse(@NotNull UUID id);

    @NotNull List<Warehouse> getAll();
}
