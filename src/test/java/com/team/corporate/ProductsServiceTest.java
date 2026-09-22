package com.team.corporate;

import com.team.corporate.entities.Category;
import com.team.corporate.entities.Product;
import com.team.corporate.exceptions.CategoryNotFoundException;
import com.team.corporate.exceptions.ProductNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ProductsServiceTest extends BaseServiceTest {

    // method create

    @Test
    @DisplayName("Создание товара: должен сохраниться с существующей категорией")
    void createProduct_shouldSaveWithCategory() {
        // given
        Category category = categoriesService.createCategory("Электроника");

        // when
        Product product = productsService.createProduct(
                "Ноутбук", "SKU-001", category.getId(), new BigDecimal("75000.00"));

        // then
        assertNotNull(product.getId());
        assertEquals("Ноутбук", product.getName());
        assertEquals("SKU-001", product.getSku());
        assertEquals(new BigDecimal("75000.00"), product.getPrice());
        assertNotNull(product.getCategory(), "Категория не должна быть null");
        assertEquals(category.getId(), product.getCategory().getId());
    }

    @Test
    @DisplayName("Создание товара: должен выбросить исключение при несуществующей категории")
    void createProduct_shouldThrow_whenCategoryNotFound() {
        // given
        UUID nonExistentCategoryId = UUID.randomUUID();

        // when and then
        assertThrows(CategoryNotFoundException.class, () ->
                productsService.createProduct("Товар", "SKU-002", nonExistentCategoryId, new BigDecimal("100.00")));
    }

    // method read

    @Test
    @DisplayName("Поиск по ID: должен вернуть товар, если он существует")
    void getProduct_shouldReturnProduct_whenExists() {
        // given
        Category category = categoriesService.createCategory("Мебель");
        Product created = productsService.createProduct("Стол", "DESK-001", category.getId(), new BigDecimal("15000.00"));

        // when
        Optional<Product> found = productsService.getProduct(created.getId());

        // then
        assertTrue(found.isPresent());
        assertEquals("Стол", found.get().getName());
    }

    @Test
    @DisplayName("Поиск по ID: должен вернуть пустой Optional, если товара нет")
    void getProduct_shouldReturnEmpty_whenNotFound() {
        // when
        Optional<Product> found = productsService.getProduct(UUID.randomUUID());

        // then
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Вывод всех товаров: должен вернуть полный список")
    void getAll_shouldReturnAllProducts() {
        // given
        Category category = categoriesService.createCategory("Канцелярия");
        productsService.createProduct("Ручка", "PEN-001", category.getId(), new BigDecimal("30.00"));
        productsService.createProduct("Бумага", "PAPER-001", category.getId(), new BigDecimal("450.00"));

        // when
        List<Product> all = productsService.getAll();

        // then
        assertEquals(2, all.size());
    }

    // method filter

    @Test
    @DisplayName("Фильтрация по категории: должен вернуть только товары этой категории")
    void getAllByCategory_shouldFilterProducts() {
        // given
        Category electronics = categoriesService.createCategory("Электроника");
        Category furniture = categoriesService.createCategory("Мебель");

        productsService.createProduct("Ноутбук", "LAP-001", electronics.getId(), new BigDecimal("75000.00"));
        productsService.createProduct("Монитор", "MON-001", electronics.getId(), new BigDecimal("25000.00"));
        productsService.createProduct("Стол", "DESK-001", furniture.getId(), new BigDecimal("15000.00"));

        // when
        List<Product> electronicsProducts = productsService.getAllByCategory(electronics.getId());
        List<Product> furnitureProducts = productsService.getAllByCategory(furniture.getId());

        // then
        assertEquals(2, electronicsProducts.size());
        assertEquals(1, furnitureProducts.size());
        assertEquals("Стол", furnitureProducts.getFirst().getName());
    }

    @Test
    @DisplayName("Фильтрация по категории: должна выбросить исключение при несуществующей категории")
    void getAllByCategory_shouldThrow_whenCategoryNotFound() {
        // when & then
        assertThrows(CategoryNotFoundException.class, () ->
                productsService.getAllByCategory(UUID.randomUUID()));
    }

    // method update

    @Test
    @DisplayName("Обновление товара: должен изменить все поля")
    void updateProduct_shouldModifyAllFields() {
        // given
        Category oldCategory = categoriesService.createCategory("Старая категория");
        Category newCategory = categoriesService.createCategory("Новая категория");
        Product product = productsService.createProduct("Старое имя", "OLD-SKU", oldCategory.getId(), new BigDecimal("100.00"));

        // when
        productsService.updateProduct(product.getId(), "Новое имя", "NEW-SKU", newCategory.getId(), new BigDecimal("200.00"));

        // then
        Product updated = productsService.getProduct(product.getId()).orElseThrow();
        assertEquals("Новое имя", updated.getName());
        assertEquals("NEW-SKU", updated.getSku());
        assertEquals(new BigDecimal("200.00"), updated.getPrice());
        assertNotNull(updated.getCategory(), "Категория не должна быть null");
        assertEquals(newCategory.getId(), updated.getCategory().getId());
    }

    @Test
    @DisplayName("Обновление товара: должен выбросить исключение при несуществующем товаре")
    void updateProduct_shouldThrow_whenProductNotFound() {
        // given
        Category category = categoriesService.createCategory("Категория");

        // when and then
        assertThrows(ProductNotFoundException.class, () ->
                productsService.updateProduct(UUID.randomUUID(), "Имя", "SKU", category.getId(), new BigDecimal("100.00")));
    }

    // method delete

    @Test
    @DisplayName("Удаление товара: должен исчезнуть из базы")
    void deleteProduct_shouldRemoveProduct() {
        // given
        Category category = categoriesService.createCategory("Категория");
        Product product = productsService.createProduct("Удалить", "DEL-001", category.getId(), new BigDecimal("100.00"));

        // when
        productsService.deleteProduct(product.getId());

        // then
        assertTrue(productsService.getProduct(product.getId()).isEmpty());
    }
}