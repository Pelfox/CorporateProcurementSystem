package com.team.corporate.core.services.impls;

import com.team.corporate.core.entities.Inventory;
import com.team.corporate.core.entities.Warehouse;
import com.team.corporate.core.repositories.*;
import com.team.corporate.core.services.StatisticsService;
import com.team.corporate.core.services.SystemStatistics;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;

public final class StatisticsServiceImpl implements StatisticsService {
    private final UsersRepository users;
    private final ProductsRepository products;
    private final CategoriesRepository categories;
    private final WarehousesRepository warehouses;
    private final OrdersRepository orders;
    private final OrderItemsRepository items;
    private final InventoriesRepository inventories;
    private final Transactions transactions;

    public StatisticsServiceImpl(@NotNull UsersRepository users,
                                 @NotNull ProductsRepository products,
                                 @NotNull CategoriesRepository categories,
                                 @NotNull WarehousesRepository warehouses,
                                 @NotNull OrdersRepository orders,
                                 @NotNull OrderItemsRepository items,
                                 @NotNull InventoriesRepository inventories,
                                 @NotNull Transactions transactions) {
        this.users = users;
        this.products = products;
        this.categories = categories;
        this.warehouses = warehouses;
        this.orders = orders;
        this.items = items;
        this.inventories = inventories;
        this.transactions = transactions;
    }

    @Override
    @NotNull
    public SystemStatistics getStatistics() {
        return transactions.execute(() -> {
            var allOrders = orders.getAll();
            long created = 0;
            long approved = 0;
            long delivered = 0;
            long cancelled = 0;
            for (var order : allOrders) {
                switch (order.getStatus()) {
                    case CREATED -> created++;
                    case APPROVED -> approved++;
                    case DELIVERED -> delivered++;
                    case CANCELLED -> cancelled++;
                }
            }
            long stockQuantity = inventories.getAll().stream().mapToLong(Inventory::getQuantity).sum();
            BigDecimal totalOrderValue = items.getAll().stream()
                    .map(item -> item.getPurchasePrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new SystemStatistics(users.getAll().size(), products.getAll().size(),
                    categories.getAll().size(), warehouses.getAll().size(), allOrders.size(),
                    created, approved, delivered, cancelled, stockQuantity, totalOrderValue);
        });
    }

    @Override
    @NotNull
    public List<WarehouseStock> getWarehouseStocks() {
        return transactions.execute(() -> {
            Map<UUID, Long> quantities = new HashMap<>();
            for (Inventory inventory : inventories.getAll()) {
                quantities.merge(inventory.getWarehouse().getId(),
                        (long) inventory.getQuantity(), Long::sum);
            }
            List<WarehouseStock> result = new ArrayList<>();
            for (Warehouse warehouse : warehouses.getAll()) {
                result.add(new WarehouseStock(warehouse.getName(),
                        quantities.getOrDefault(warehouse.getId(), 0L)));
            }
            result.sort(Comparator.comparing(WarehouseStock::warehouseName));
            return result;
        });
    }
}
