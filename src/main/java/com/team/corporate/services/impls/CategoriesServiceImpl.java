package com.team.corporate.services.impls;

import com.team.corporate.entities.Category;
import com.team.corporate.exceptions.CategoryNotFoundException;
import com.team.corporate.repositories.CategoriesRepository;
import com.team.corporate.repositories.ProductsRepository;
import com.team.corporate.repositories.Transactions;
import com.team.corporate.services.CategoriesService;
import com.team.corporate.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CategoriesServiceImpl implements CategoriesService {
    private final CategoriesRepository categoriesRepository;

    private final ProductsRepository productsRepository;
    private final Transactions transactions;

    public CategoriesServiceImpl(@NotNull CategoriesRepository categoriesRepository,
                                 @NotNull ProductsRepository productsRepository,
                                 @NotNull Transactions transactions) {
        this.categoriesRepository = categoriesRepository;
        this.productsRepository = productsRepository;
        this.transactions = transactions;
    }

    @Override
    @NotNull
    public Category createCategory(@NotNull String categoryName) {
        EntityValidation.requireNonNull(categoryName, "Название категории");
        return categoriesRepository.add(new Category(categoryName));
    }

    @Override
    @NotNull
    public List<Category> getAll() {
        return categoriesRepository.getAll();
    }

    @Override
    public void deleteCategory(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        transactions.execute(() -> {
            boolean used = productsRepository.getAll().stream()
                    .anyMatch(product -> product.getCategory() != null && product.getCategory().getId().equals(id));
            if (used) {
                throw new IllegalArgumentException("Сначала удалите или перенесите товары этой категории.");
            }
            if (categoriesRepository.getById(id).isPresent()) {
                categoriesRepository.delete(id);
            }
        });
    }

    @Override
    @NotNull
    public Optional<Category> getCategory(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return categoriesRepository.getById(id);
    }

    @Override
    @NotNull
    public Category updateCategory(@NotNull UUID id, @NotNull String name) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        EntityValidation.requireNonNull(name, "Название");
        Category categoryToUpdate = categoriesRepository.getById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Категория с указанным идентификатором не найдена."));
        categoryToUpdate.setName(name);
        return categoriesRepository.update(categoryToUpdate);
    }
}
