package com.team.corporate.services.impls;

import com.team.corporate.entities.Category;
import com.team.corporate.entities.Product;
import com.team.corporate.exceptions.CategoryNotFoundException;
import com.team.corporate.exceptions.ProductNotFoundException;
import com.team.corporate.repositories.*;
import com.team.corporate.services.ProductsService;
import com.team.corporate.utils.EntityValidation;
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
        EntityValidation.requireNonNull(name, "Название");
        EntityValidation.requireNonNull(sku, "Артикул");
        EntityValidation.requireNonNull(categoryId, "Идентификатор категории");
        EntityValidation.requireNonNull(price, "Цена");
        Category category = categoriesRepository.getById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория с указанным идентификатором не найдена"));
        Product newProduct = new Product(sku, name, category, price);
        requireAvailableSku(newProduct.getSku(), null);
        return productsRepository.add(newProduct);
    }

    @Override
    public void deleteProduct(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
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
        EntityValidation.requireNonNull(id, "Идентификатор");
        Product product = productsRepository.getById(id)
                .orElseThrow(() -> new ProductNotFoundException("Продукт с указанным идентификатором не найден"));
        if (name != null) {
            EntityValidation.requireText(name, "Название товара");
        }
        if (sku != null) {
            requireAvailableSku(EntityValidation.requireText(sku, "Артикул"), id);
        }
        if (price != null) {
            EntityValidation.requireMoney(price, "Цена");
        }
        Category category = categoryId == null ? null : categoriesRepository.getById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория с указанным идентификатором не найдена"));
        if (name != null) {
            product.setName(name);
        }
        if (sku != null) {
            product.setSku(sku);
        }
        if (categoryId != null) {
            product.setCategory(category);
        }
        if (price != null) {
            product.setPrice(price);
        }
        return productsRepository.update(product);
    }

    private void requireAvailableSku(String sku, UUID currentId) {
        productsRepository.getBySKU(sku).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new IllegalArgumentException("Товар с таким артикулом уже существует.");
            }
        });
    }

    @Override
    @NotNull
    public Optional<Product> getProduct(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return productsRepository.getById(id);
    }

    @Override
    @NotNull
    public List<Product> getAllByCategory(@NotNull UUID categoryId) {
        EntityValidation.requireNonNull(categoryId, "Идентификатор категории");
        categoriesRepository.getById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория с указанным идентификатором не найдена"));
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
