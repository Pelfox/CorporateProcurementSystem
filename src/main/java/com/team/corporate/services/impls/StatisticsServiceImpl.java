package com.team.corporate.services.impls;

import com.team.corporate.entities.Inventory;
import com.team.corporate.repositories.*;
import com.team.corporate.services.StatisticsService;
import com.team.corporate.services.SystemStatistics;
import org.jetbrains.annotations.NotNull;

public final class StatisticsServiceImpl implements StatisticsService {
    private final UsersRepository users;
    private final ProductsRepository products;
    private final CategoriesRepository categories;
    private final WarehousesRepository warehouses;
    private final OrdersRepository orders;
    private final InventoriesRepository inventories;
    private final Transactions transactions;

    public StatisticsServiceImpl(@NotNull UsersRepository users,
                                 @NotNull ProductsRepository products,
                                 @NotNull CategoriesRepository categories,
                                 @NotNull WarehousesRepository warehouses,
                                 @NotNull OrdersRepository orders,
                                 @NotNull InventoriesRepository inventories,
                                 @NotNull Transactions transactions) {
        this.users = users;
        this.products = products;
        this.categories = categories;
        this.warehouses = warehouses;
        this.orders = orders;
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
            return new SystemStatistics(users.getAll().size(), products.getAll().size(),
                    categories.getAll().size(), warehouses.getAll().size(), allOrders.size(),
                    created, approved, delivered, cancelled, stockQuantity);
        });
    }
}
