package com.team.corporate.test.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryValidationTest extends BaseServiceTest {
    @Test
    void categoryNameAcceptsExactly255Characters() {
        String name = "Я".repeat(255);
        var category = categoriesService.createCategory(name);
        assertEquals(name, categoriesService.getCategory(category.getId()).orElseThrow().getName());
    }

    @Test
    void categoryNameRejects256CharactersOnCreate() {
        assertThrows(IllegalArgumentException.class, () -> categoriesService.createCategory("Я".repeat(256)));
        assertTrue(categoriesService.getAll().isEmpty());
    }

    @Test
    void categoryNameIsValidatedWhenEditing() {
        var category = categoriesService.createCategory("Категория");
        String name = "Ю".repeat(255);
        categoriesService.updateCategory(category.getId(), name);

        assertThrows(IllegalArgumentException.class,
                () -> categoriesService.updateCategory(category.getId(), "Ю".repeat(256)));
        assertThrows(IllegalArgumentException.class,
                () -> categoriesService.updateCategory(category.getId(), "Ю".repeat(10_000)));
        assertEquals(name, categoriesService.getCategory(category.getId()).orElseThrow().getName());
    }
}
