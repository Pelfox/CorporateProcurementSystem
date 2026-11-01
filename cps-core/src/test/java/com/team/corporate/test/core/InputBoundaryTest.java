package com.team.corporate.test.core;

import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.services.OrdersService.Line;
import com.team.corporate.core.utils.EntityValidation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputBoundaryTest extends BaseServiceTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\r\n", "\u2003"})
    void requiredNamesRejectBlankInput(String value) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> usersService.createUser(value, UserRole.USER));
        Assertions.assertThrows(IllegalArgumentException.class, () -> categoriesService.createCategory(value));
        Assertions.assertThrows(IllegalArgumentException.class, () -> warehousesService.createWarehouse(value, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad\u0000text", "bad\u0001text", "bad\u001btext", "bad\u007ftext"})
    void textRejectsControlCharactersBeforeDatabaseWrite(String value) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> EntityValidation.optionalText(value, "Поле"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> usersService.createUser(value, UserRole.USER));
        Assertions.assertThrows(IllegalArgumentException.class, () -> warehousesService.createWarehouse("Склад", value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100000000", "0.001", "1E+1000000", "1E-1000000"})
    void moneyRejectsValuesOutsideDatabasePrecisionAndScale(String value) {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> EntityValidation.requireMoney(new BigDecimal(value), "Цена"));
    }

    @Test
    void productBoundaryAndFailedEditPreserveStoredFields() {
        var category = categoriesService.createCategory("Категория");
        String maximum = "Я".repeat(255);
        var product = productsService.createProduct(maximum, "BOUNDARY-001", category.getId(), EntityValidation.MAX_MONEY);
        assertEquals(maximum, productsService.getProduct(product.getId()).orElseThrow().getName());
        for (int length : new int[]{256, 10_000}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> productsService.updateProduct(product.getId(),
                    "Новое имя", "BOUNDARY-002", category.getId(), new BigDecimal("100000000")));
            Assertions.assertThrows(IllegalArgumentException.class, () -> productsService.updateProduct(product.getId(),
                    "Я".repeat(length), "BOUNDARY-002", category.getId(), BigDecimal.ONE));
        }
        var saved = productsService.getProduct(product.getId()).orElseThrow();
        assertEquals(maximum, saved.getName());
        assertEquals("BOUNDARY-001", saved.getSku());
        assertEquals(EntityValidation.MAX_MONEY, saved.getPrice());
    }

    @Test
    void orderQuantityAndNoteLimitsCannotBeBypassedThroughServices() {
        var buyer = usersService.createUser("Покупатель", UserRole.USER);
        var category = categoriesService.createCategory("Категория");
        var product = productsService.createProduct("Товар", "BOUNDARY-001", category.getId(), BigDecimal.ONE);
        var warehouse = warehousesService.createWarehouse("Склад", null);
        inventoriesService.placeProductInWarehouse(product.getId(), warehouse.getId(), Integer.MAX_VALUE);
        String notes = "Я".repeat(255);
        var id = ordersService.createOrder(buyer.getId(), List.of(new Line(product.getId(), 100_000)), notes);
        assertEquals(notes, ordersService.getOrder(id).orElseThrow().getNotes());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Line(product.getId(), 100_001));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ordersService.createOrder(buyer.getId(),
                List.of(new Line(product.getId(), 100_000), new Line(product.getId(), 1)), null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ordersService.addItem(buyer.getId(), id, new Line(product.getId(), 1)));
        var line = orderItemsService.getAllByOrder(id).getFirst();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> orderItemsService.updateOrderItem(line.getId(), 100_001, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ordersService.updateNotes(buyer.getId(), id, "Я".repeat(256)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ordersService.updateNotes(buyer.getId(), id, "Я".repeat(10_000)));
        assertEquals(1, ordersService.getAll().size());
        assertEquals(100_000, orderItemsService.getAllByOrder(id).getFirst().getQuantity());
        assertEquals(notes, ordersService.getOrder(id).orElseThrow().getNotes());
        assertEquals(Integer.MAX_VALUE, inventoriesService.getAll().getFirst().getQuantity());
    }
}
