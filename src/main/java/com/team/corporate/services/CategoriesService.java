package com.team.corporate.services;

import com.team.corporate.entities.Category;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface CategoriesService {

    @NotNull Category createCategory(@NotNull String categoryName);

    @NotNull Category getAll();

    void deleteCategory();

    @NotNull Optional<Category> getCategory(@NotNull UUID id);

    @NotNull Category updateCategory(@NotNull UUID id, @NotNull String name);
}
