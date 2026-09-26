package com.team.corporate;

import com.team.corporate.entities.*;
import com.team.corporate.services.CsvExportService;
import com.team.corporate.services.impls.CsvExportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CsvExportServiceTest extends BaseServiceTest {
    @TempDir
    Path directory;
    private CsvExportService exporter;

    @BeforeEach
    void setUpExporter() {
        exporter = new CsvExportServiceImpl(usersRepository, categoriesRepository, productsRepository,
                warehousesRepository, inventoriesRepository, ordersRepository, orderItemsRepository,
                auditLogsRepository, transactions);
    }

    @Test
    void exportsEveryEntityWithRelationshipsAndStoredValues() throws Exception {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        var buyer = usersService.createUser("Покупатель", UserRole.USER);
        var category = categoriesService.createCategory("Канцелярия");
        var product = productsService.createProduct("Бумага, \"А4\"", "PAPER", category.getId(), new BigDecimal("120.50"));
        var warehouse = warehousesService.createWarehouse("Главный склад", null);
        var stock = inventoriesService.placeProductInWarehouse(product.getId(), warehouse.getId(), 10);
        var order = ordersService.createOrder(buyer.getId(), OrderStatus.CREATED, "Срочно, пожалуйста\nК утру");
        var item = orderItemsService.createOrderItemForOrder(order.getId(), product.getId(), 2, new BigDecimal("99.90"));
        ordersService.changeStatus(manager.getId(), order.getId(), OrderStatus.APPROVED);
        var savedOrder = ordersService.getOrder(order.getId()).orElseThrow();
        var log = auditLogsService.getAllByOrder(order.getId()).getFirst();

        Path result = exporter.exportAll(manager.getId(), directory);

        assertEquals(directory, result.getParent());
        try (var files = Files.list(result)) {
            assertEquals(Set.of("users.csv", "categories.csv", "products.csv", "warehouses.csv",
                            "inventories.csv", "orders.csv", "order_items.csv", "audit_logs.csv"),
                    files.map(p -> p.getFileName().toString()).collect(Collectors.toSet()));
        }
        String users = Files.readString(result.resolve("users.csv"));
        assertTrue(users.startsWith("id,username,user_role,created_at\r\n"));
        assertTrue(users.contains(manager.getId() + ",Менеджер,MANAGER," + manager.getCreatedAt() + "\r\n"));
        assertTrue(users.contains(buyer.getId() + ",Покупатель,USER," + buyer.getCreatedAt() + "\r\n"));
        assertEquals("id,name\r\n" + category.getId() + ",Канцелярия\r\n",
                Files.readString(result.resolve("categories.csv")));
        assertEquals("id,sku,name,category_id,price\r\n" + product.getId()
                        + ",PAPER,\"Бумага, \"\"А4\"\"\"," + category.getId() + ",120.50\r\n",
                Files.readString(result.resolve("products.csv")));
        assertEquals("id,name,location\r\n" + warehouse.getId() + ",Главный склад,\r\n",
                Files.readString(result.resolve("warehouses.csv")));
        assertEquals("id,product_id,warehouse_id,quantity\r\n" + stock.getId() + "," + product.getId()
                        + "," + warehouse.getId() + ",10\r\n",
                Files.readString(result.resolve("inventories.csv")));
        assertEquals("id,user_id,order_status,notes,created_at,updated_at\r\n" + order.getId() + ","
                        + buyer.getId() + ",APPROVED,\"Срочно, пожалуйста\nК утру\"," + savedOrder.getCreatedAt()
                        + "," + savedOrder.getUpdatedAt() + "\r\n",
                Files.readString(result.resolve("orders.csv")));
        assertEquals("id,order_id,product_id,quantity,purchase_price\r\n" + item.getId() + "," + order.getId()
                        + "," + product.getId() + ",2,99.90\r\n",
                Files.readString(result.resolve("order_items.csv")));
        assertEquals("id,order_id,changed_by,old_status,new_status,changed_at\r\n" + log.getId() + ","
                        + order.getId() + "," + manager.getId() + ",CREATED,APPROVED," + log.getChangedAt() + "\r\n",
                Files.readString(result.resolve("audit_logs.csv")));
    }

    @Test
    void exportsEmptyTablesAndCreatesMissingParentDirectories() throws Exception {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        Path result = exporter.exportAll(manager.getId(), directory.resolve("new/nested"));
        for (String name : List.of("categories", "products", "warehouses", "inventories", "orders", "order_items", "audit_logs")) {
            String csv = Files.readString(result.resolve(name + ".csv"));
            assertTrue(csv.startsWith("id,"), name);
            assertEquals(1, csv.lines().count(), name);
            assertTrue(csv.endsWith("\r\n"), name);
        }
    }

    @Test
    void exportsProductWithoutCategoryAndOrderWithoutNotes() throws Exception {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        var product = productsRepository.add(new Product("SKU", "Товар", null, new BigDecimal("0.00")));
        var order = ordersService.createOrder(manager.getId(), OrderStatus.CREATED, null);
        Path result = exporter.exportAll(manager.getId(), directory);
        assertTrue(Files.readString(result.resolve("products.csv")).contains(product.getId() + ",SKU,Товар,,0.00\r\n"));
        assertTrue(Files.readString(result.resolve("orders.csv")).contains(order.getId() + "," + manager.getId() + ",CREATED,,"));
    }

    @Test
    void rejectsRegularAndUnknownUsersBeforeCreatingFiles() {
        var user = usersService.createUser("Покупатель", UserRole.USER);
        Path target = directory.resolve("unauthorized");
        assertThrows(IllegalArgumentException.class, () -> exporter.exportAll(user.getId(), target));
        assertThrows(IllegalArgumentException.class, () -> exporter.exportAll(UUID.randomUUID(), target));
        assertFalse(Files.exists(target));
    }

    @Test
    void repeatedExportsPreservePreviousFiles() throws Exception {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        Path first = exporter.exportAll(manager.getId(), directory);
        String original = Files.readString(first.resolve("categories.csv"));
        categoriesService.createCategory("Новая категория");
        Path second = exporter.exportAll(manager.getId(), directory);
        assertNotEquals(first, second);
        assertEquals(original, Files.readString(first.resolve("categories.csv")));
        assertTrue(Files.readString(second.resolve("categories.csv")).contains("Новая категория"));
    }

    @Test
    void reportsFileSystemFailureWithoutOverwritingFile() throws Exception {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        Path target = directory.resolve("file");
        Files.writeString(target, "original");
        assertThrows(IOException.class, () -> exporter.exportAll(manager.getId(), target));
        assertEquals("original", Files.readString(target));
    }

    @Test
    void rejectsEmptyAndControlCharacterPaths() {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        for (Path path : List.of(Path.of(""), Path.of("   "), directory.resolve("bad\nfolder"))) {
            assertThrows(IllegalArgumentException.class, () -> exporter.exportAll(manager.getId(), path));
        }
        assertThrows(IllegalArgumentException.class, () -> exporter.exportAll(manager.getId(), null));
        assertThrows(IllegalArgumentException.class, () -> exporter.exportAll(null, directory));
    }

    @Test
    void protectsFormulaValuesWithoutChangingDatabase() throws Exception {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        var category = categoriesService.createCategory("=1+1");
        Path result = exporter.exportAll(manager.getId(), directory);
        assertTrue(Files.readString(result.resolve("categories.csv")).contains(
                category.getId() + ",'=1+1\r\n"));
        assertEquals("=1+1", categoriesRepository.getById(category.getId()).orElseThrow().getName());
    }

    @Test
    void validationFailureRemovesPartialExportAndPreservesEarlierExports() throws Exception {
        var manager = usersService.createUser("Менеджер", UserRole.MANAGER);
        Path previous = exporter.exportAll(manager.getId(), directory);
        String original = Files.readString(previous.resolve("orders.csv"));
        ordersService.createOrder(manager.getId(), OrderStatus.CREATED, "bad\u0000note");

        var error = assertThrows(IllegalArgumentException.class, () -> exporter.exportAll(manager.getId(), directory));
        assertTrue(error.getMessage().contains("orders.csv"));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(previous), files.toList());
        }
        assertEquals(original, Files.readString(previous.resolve("orders.csv")));
    }
}
