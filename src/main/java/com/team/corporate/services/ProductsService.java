package com.team.corporate.services;

import com.team.corporate.entities.Category;
import com.team.corporate.entities.Product;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductsService {

    @NotNull Product createProduct(@NotNull String name, @NotNull String sku, @NotNull Category category);

    void deleteProduct(@NotNull UUID id);

    @NotNull Product updateProduct(@NotNull UUID id, String name, String sku, Category category);

    @NotNull Optional<Product> getProduct(@NotNull UUID id);

    @NotNull List<Product> getAllByCategory(@NotNull Category category);

    @NotNull List<Product> getAll();

    boolean checkIfExists(@NotNull UUID id);

}
