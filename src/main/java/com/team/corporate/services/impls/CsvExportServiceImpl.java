package com.team.corporate.services.impls;

import com.team.corporate.entities.UserRole;
import com.team.corporate.repositories.*;
import com.team.corporate.services.CsvExportService;
import com.team.corporate.utils.CsvWriter;
import com.team.corporate.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class CsvExportServiceImpl implements CsvExportService {
    private final UsersRepository users;
    private final CategoriesRepository categories;
    private final ProductsRepository products;
    private final WarehousesRepository warehouses;
    private final InventoriesRepository inventories;
    private final OrdersRepository orders;
    private final OrderItemsRepository items;
    private final AuditLogsRepository auditLogs;
    private final Transactions transactions;

    public CsvExportServiceImpl(@NotNull UsersRepository users,
                                @NotNull CategoriesRepository categories,
                                @NotNull ProductsRepository products,
                                @NotNull WarehousesRepository warehouses,
                                @NotNull InventoriesRepository inventories,
                                @NotNull OrdersRepository orders,
                                @NotNull OrderItemsRepository items,
                                @NotNull AuditLogsRepository auditLogs,
                                @NotNull Transactions transactions) {
        this.users = users;
        this.categories = categories;
        this.products = products;
        this.warehouses = warehouses;
        this.inventories = inventories;
        this.orders = orders;
        this.items = items;
        this.auditLogs = auditLogs;
        this.transactions = transactions;
    }

    @Override
    @NotNull
    public Path exportAll(@NotNull UUID managerId, @NotNull Path directory) throws IOException {
        EntityValidation.requireNonNull(managerId, "Идентификатор менеджера");
        EntityValidation.requireNonNull(directory, "Папка экспорта");
        List<Table> tables = transactions.execute(() -> {
            var manager = users.getById(managerId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));
            if (manager.getUserRole() != UserRole.MANAGER) {
                throw new IllegalArgumentException("Доступ только для менеджера.");
            }
            return readTables();
        });

        Path parent = Files.createDirectories(directory.toAbsolutePath().normalize());
        Path destination = Files.createTempDirectory(parent, "csv-export-");
        try {
            for (Table table : tables) {
                CsvWriter.write(destination.resolve(table.name()), table.headers(), table.rows());
            }
            return destination;
        } catch (IOException | RuntimeException e) {
            for (Table table : tables) {
                try {
                    Files.deleteIfExists(destination.resolve(table.name()));
                } catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }
    }

    private List<Table> readTables() {
        return List.of(
                new Table("users.csv", List.of("id", "username", "user_role", "created_at"),
                        users.getAll().stream().map(u -> new Object[]{
                                u.getId(), u.getUsername(), u.getUserRole(), u.getCreatedAt()}).toList()),
                new Table("categories.csv", List.of("id", "name"),
                        categories.getAll().stream().map(c -> new Object[]{c.getId(), c.getName()}).toList()),
                new Table("products.csv", List.of("id", "sku", "name", "category_id", "price"),
                        products.getAll().stream().map(p -> new Object[]{p.getId(), p.getSku(), p.getName(),
                                p.getCategory() == null ? null : p.getCategory().getId(), p.getPrice()}).toList()),
                new Table("warehouses.csv", List.of("id", "name", "location"),
                        warehouses.getAll().stream().map(w -> new Object[]{w.getId(), w.getName(), w.getLocation()}).toList()),
                new Table("inventories.csv", List.of("id", "product_id", "warehouse_id", "quantity"),
                        inventories.getAll().stream().map(i -> new Object[]{
                                i.getId(), i.getProduct().getId(), i.getWarehouse().getId(), i.getQuantity()}).toList()),
                new Table("orders.csv", List.of("id", "user_id", "order_status", "notes", "created_at", "updated_at"),
                        orders.getAll().stream().map(o -> new Object[]{o.getId(), o.getUser().getId(),
                                o.getStatus(), o.getNotes(), o.getCreatedAt(), o.getUpdatedAt()}).toList()),
                new Table("order_items.csv", List.of("id", "order_id", "product_id", "quantity", "purchase_price"),
                        items.getAll().stream().map(i -> new Object[]{i.getId(), i.getOrder().getId(),
                                i.getProduct().getId(), i.getQuantity(), i.getPurchasePrice()}).toList()),
                new Table("audit_logs.csv", List.of("id", "order_id", "changed_by", "old_status", "new_status", "changed_at"),
                        auditLogs.getAll().stream().map(a -> new Object[]{a.getId(), a.getOrder().getId(),
                                a.getChangedBy().getId(), a.getOldStatus(), a.getNewStatus(), a.getChangedAt()}).toList())
        );
    }

    private record Table(String name, List<String> headers, List<Object[]> rows) {
    }
}
