package com.team.corporate.services.impls;

import com.team.corporate.entities.Category;
import com.team.corporate.exceptions.CategoryNotFoundException;
import com.team.corporate.repositories.CategoriesRepository;
import com.team.corporate.services.CategoriesService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CategoriesServiceImpl implements CategoriesService {
    private final CategoriesRepository categoriesRepository;

    public CategoriesServiceImpl(@NotNull CategoriesRepository categoriesRepository) {
        this.categoriesRepository = categoriesRepository;
    }

    @Override
    public @NotNull Category createCategory(@NotNull String categoryName) {
        return categoriesRepository.add(new Category(categoryName));
    }

    @Override
    public @NotNull List<Category> getAll() {
        return categoriesRepository.getAll();
    }

    @Override
    public void deleteCategory(@NotNull UUID id) {
        Optional<Category> category = categoriesRepository.getById(id);
        if (category.isPresent()) {
            categoriesRepository.delete(id);
        }
    }

    @Override
    public @NotNull Optional<Category> getCategory(@NotNull UUID id) {
        return categoriesRepository.getById(id);
    }

    @Override
    public @NotNull Category updateCategory(@NotNull UUID id, @NotNull String name) {
        Category categoryToUpdate = categoriesRepository.getById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Категория с указанным ID не найдена."));
        categoryToUpdate.setName(name);
        return categoriesRepository.update(categoryToUpdate);
    }
}
