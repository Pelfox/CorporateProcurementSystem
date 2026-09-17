package com.team.corporate.repositories;

import com.team.corporate.models.Category;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface CategoriesRepository {
    @NotNull Category add(@NotNull Category category);

    @NotNull List<Category> getAll();

    Optional<Category> getById(int id);

    @NotNull Category update(@NotNull Category category);

    void delete(int id);
}
