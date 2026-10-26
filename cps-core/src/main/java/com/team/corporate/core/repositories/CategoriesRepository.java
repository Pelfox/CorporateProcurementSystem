package com.team.corporate.core.repositories;

import com.team.corporate.core.entities.Category;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriesRepository {
    @NotNull
    Category add(@NotNull Category category);

    @NotNull
    List<Category> getAll();

    @NotNull
    Optional<Category> getById(@NotNull UUID id);

    @NotNull
    Category update(@NotNull Category category);

    void delete(@NotNull UUID id);
}
