package com.team.corporate.test.core;

import com.team.corporate.core.entities.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormTextValidationTest extends BaseServiceTest {
    @Test
    void warehouseAccepts255CharactersInBothFields() {
        String name = "С".repeat(255);
        String address = "А".repeat(255);
        var warehouse = warehousesService.createWarehouse(name, address);
        var saved = warehousesService.getWarehouse(warehouse.getId()).orElseThrow();
        assertEquals(name, saved.getName());
        assertEquals(address, saved.getLocation());

        warehousesService.saveWarehouse(warehouse.getId(), address, name);
        saved = warehousesService.getWarehouse(warehouse.getId()).orElseThrow();
        assertEquals(address, saved.getName());
        assertEquals(name, saved.getLocation());
    }

    @Test
    void warehouseRejectsOversizedNameAndAddressOnCreate() {
        for (int length : new int[]{256, 10_000}) {
            String oversized = "Я".repeat(length);
            assertThrows(IllegalArgumentException.class,
                    () -> warehousesService.createWarehouse(oversized, "Адрес"));
            assertThrows(IllegalArgumentException.class,
                    () -> warehousesService.createWarehouse("Склад", oversized));
        }
        assertTrue(warehousesService.getAll().isEmpty());
    }

    @Test
    void warehouseRejectsOversizedEditsWithoutChangingEitherField() {
        var warehouse = warehousesService.createWarehouse("Склад", "Адрес");
        for (int length : new int[]{256, 10_000}) {
            String oversized = "Я".repeat(length);
            assertThrows(IllegalArgumentException.class,
                    () -> warehousesService.saveWarehouse(warehouse.getId(), oversized, "Новый адрес"));
            assertThrows(IllegalArgumentException.class,
                    () -> warehousesService.saveWarehouse(warehouse.getId(), "Новое имя", oversized));
            assertThrows(IllegalArgumentException.class,
                    () -> warehousesService.updateWarehouse(warehouse.getId(), "Новое имя", oversized));
        }
        var saved = warehousesService.getWarehouse(warehouse.getId()).orElseThrow();
        assertEquals("Склад", saved.getName());
        assertEquals("Адрес", saved.getLocation());
    }

    @Test
    void usernameAccepts255CharactersOnCreateAndEdit() {
        String username = "И".repeat(255);
        var user = usersService.createUser(username, UserRole.USER);
        assertEquals(username, usersService.getUser(user.getId()).orElseThrow().getUsername());
        String edited = "Ю".repeat(255);
        usersService.updateUser(user.getId(), edited, UserRole.MANAGER);
        assertEquals(edited, usersService.getUser(user.getId()).orElseThrow().getUsername());
    }

    @Test
    void usernameRejectsOversizedCreationAndEditing() {
        var user = usersService.createUser("Пользователь", UserRole.USER);
        for (int length : new int[]{256, 10_000}) {
            String oversized = "Я".repeat(length);
            assertThrows(IllegalArgumentException.class,
                    () -> usersService.createUser(oversized, UserRole.USER));
            assertThrows(IllegalArgumentException.class,
                    () -> usersService.updateUser(user.getId(), oversized, UserRole.MANAGER));
        }
        var saved = usersService.getUser(user.getId()).orElseThrow();
        assertEquals("Пользователь", saved.getUsername());
        assertEquals(UserRole.USER, saved.getUserRole());
        assertEquals(1, usersService.getAll().size());
    }
}
