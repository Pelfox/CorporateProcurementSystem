package com.team.corporate.repositories;


import com.team.corporate.models.Product;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductsRepository {
    @NotNull Product add(@NotNull Product product);

    @NotNull List<Product> getAll();

    Optional<Product> getById(@NotNull UUID id);

    Optional<Product> getBySKU(@NotNull String sku);

    @NotNull Product update(@NotNull Product product);

    void delete(@NotNull UUID id);
}
