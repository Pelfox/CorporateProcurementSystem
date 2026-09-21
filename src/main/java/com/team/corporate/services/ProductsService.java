package com.team.corporate.services;

import com.team.corporate.entities.Product;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductsService {
    @NotNull
    Product createProduct(@NotNull String name, @NotNull String sku, @NotNull UUID categoryId, @NotNull BigDecimal price);

    void deleteProduct(@NotNull UUID id);

    @NotNull
    Product updateProduct(@NotNull UUID id, @Nullable String name, @Nullable String sku, @Nullable UUID categoryId, @Nullable BigDecimal price);

    @NotNull
    Optional<Product> getProduct(@NotNull UUID id);

    @NotNull
    List<Product> getAllByCategory(@NotNull UUID categoryId);

    @NotNull
    List<Product> getAll();
}
