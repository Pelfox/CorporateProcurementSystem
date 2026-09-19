package com.team.corporate.services.impls;

import com.team.corporate.entities.Category;
import com.team.corporate.entities.Product;
import com.team.corporate.repositories.CategoriesRepository;
import com.team.corporate.repositories.ProductsRepository;
import com.team.corporate.services.ProductsService;
import jakarta.persistence.EntityNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ProductsServiceImpl implements ProductsService {
    private final ProductsRepository productsRepository;
    private final CategoriesRepository categoriesRepository;

    public ProductsServiceImpl(ProductsRepository productsRepository, CategoriesRepository categoriesRepository) {
        this.productsRepository = productsRepository;
        this.categoriesRepository = categoriesRepository;
    }

    @Override
    public @NotNull Product createProduct(@NotNull String name, @NotNull String sku, @NotNull UUID categoryId, @NotNull BigDecimal price) {
        Category category = categoriesRepository.getById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Категория с указанным id не найдена"));

        Product newProduct = new Product(sku, name, category, price);
        return productsRepository.add(newProduct);
    }

    @Override
    public void deleteProduct(@NotNull UUID id) {
        productsRepository.delete(id);
    }

    @Override
    public @NotNull Product updateProduct(@NotNull UUID id, @Nullable String name, @Nullable String sku, @Nullable UUID categoryId, @Nullable BigDecimal price) {
        Product product = productsRepository.getById(id)
                .orElseThrow(() -> new EntityNotFoundException("Продукт с указанным id не найден"));

        if (name != null) {
            product.setName(name);
        }
        if (sku != null) {
            product.setSku(sku);
        }
        if (categoryId != null) {
            Category category = categoriesRepository.getById(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Категория с указанным id не найдена"));
            product.setCategory(category);
        }
        if (price != null) {
            product.setPrice(price);
        }
        return productsRepository.update(product);
    }

    @Override
    public @NotNull Optional<Product> getProduct(@NotNull UUID id) {
        return productsRepository.getById(id);
    }

    @Override
    public @NotNull List<Product> getAllByCategory(@NotNull UUID categoryId) {
        categoriesRepository.getById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Категория с указанным id не найдена"));

        return productsRepository.getAll() // Требуется реализовать отдельный метод в ProductsRepository.
                .stream().filter(product -> product.getCategory().getId().equals(categoryId))
                .toList();
    }

    @Override
    public @NotNull List<Product> getAll() {
        return productsRepository.getAll();
    }
}
