package com.team.corporate.services.impls;

import com.team.corporate.entities.Category;
import com.team.corporate.entities.Product;
import com.team.corporate.exceptions.CategoryNotFoundException;
import com.team.corporate.exceptions.ProductNotFoundException;
import com.team.corporate.repositories.*;
import com.team.corporate.services.ProductsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ProductsServiceImpl implements ProductsService {
    private final ProductsRepository productsRepository;
    private final CategoriesRepository categoriesRepository;

    private final OrderItemsRepository orderItemsRepository;
    private final InventoriesRepository inventoriesRepository;
    private final Transactions transactions;

    public ProductsServiceImpl(@NotNull ProductsRepository productsRepository,
                               @NotNull CategoriesRepository categoriesRepository,
                               @NotNull OrderItemsRepository orderItemsRepository,
                               @NotNull InventoriesRepository inventoriesRepository,
                               @NotNull Transactions transactions) {
        this.productsRepository = productsRepository;
        this.categoriesRepository = categoriesRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.inventoriesRepository = inventoriesRepository;
        this.transactions = transactions;
    }

    @Override
    @NotNull
    public Product createProduct(@NotNull String name,
                                 @NotNull String sku,
                                 @NotNull UUID categoryId,
                                 @NotNull BigDecimal price) {
        Category category = categoriesRepository.getById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория с указанным ID не найдена"));
        Product newProduct = new Product(sku, name, category, price);
        return productsRepository.add(newProduct);
    }

    @Override
    public void deleteProduct(@NotNull UUID id) {
        transactions.execute(() -> {
            if (!orderItemsRepository.getByProductId(id).isEmpty()) {
                throw new IllegalArgumentException("Товар используется в истории заказов. Можно удалить его остаток со склада.");
            }
            for (var inventory : inventoriesRepository.getByProductId(id)) {
                inventoriesRepository.delete(inventory.getId());
            }
            productsRepository.delete(id);
        });
    }

    @Override
    @NotNull
    public Product updateProduct(@NotNull UUID id,
                                 @Nullable String name,
                                 @Nullable String sku,
                                 @Nullable UUID categoryId,
                                 @Nullable BigDecimal price) {
        Product product = productsRepository.getById(id)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным ID не найден"));
        if (name != null) {
            product.setName(name);
        }
        if (sku != null) {
            product.setSku(sku);
        }
        if (categoryId != null) {
            Category category = categoriesRepository.getById(categoryId)
                    .orElseThrow(() -> new CategoryNotFoundException("Категория с указанным ID не найдена"));
            product.setCategory(category);
        }
        if (price != null) {
            product.setPrice(price);
        }
        return productsRepository.update(product);
    }

    @Override
    @NotNull
    public Optional<Product> getProduct(@NotNull UUID id) {
        return productsRepository.getById(id);
    }

    @Override
    @NotNull
    public List<Product> getAllByCategory(@NotNull UUID categoryId) {
        categoriesRepository.getById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория с указанным ID не найдена"));
        return productsRepository.getAll()
                .stream()
                .filter(product -> product.getCategory() != null && product.getCategory().getId().equals(categoryId))
                .toList();
    }

    @Override
    @NotNull
    public List<Product> getAll() {
        return productsRepository.getAll();
    }
}
