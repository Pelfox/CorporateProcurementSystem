package com.team.corporate.core.services.impls;

import com.team.corporate.core.dto.*;
import com.team.corporate.core.entities.Inventory;
import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.entities.Warehouse;
import com.team.corporate.core.repositories.*;
import com.team.corporate.core.services.StatisticsService;
import com.team.corporate.core.services.SystemStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @Nullable
    private final StatisticsRepository statisticsRepository;

    private static final int LOW_STOCK_THRESHOLD = 5;

    public StatisticsServiceImpl(@NotNull UsersRepository users,
                                 @NotNull ProductsRepository products,
                                 @NotNull CategoriesRepository categories,
                                 @NotNull WarehousesRepository warehouses,
                                 @NotNull OrdersRepository orders,
                                 @NotNull OrderItemsRepository items,
                                 @NotNull InventoriesRepository inventories,
                                 @NotNull Transactions transactions) {
        this(users, products, categories, warehouses, orders, items, inventories, transactions, null);
    }

    public StatisticsServiceImpl(@NotNull UsersRepository users,
                                 @NotNull ProductsRepository products,
                                 @NotNull CategoriesRepository categories,
                                 @NotNull WarehousesRepository warehouses,
                                 @NotNull OrdersRepository orders,
                                 @NotNull OrderItemsRepository items,
                                 @NotNull InventoriesRepository inventories,
                                 @NotNull Transactions transactions,
                                 @Nullable StatisticsRepository statisticsRepository) {
        this.users = users;
        this.products = products;
        this.categories = categories;
        this.warehouses = warehouses;
        this.orders = orders;
        this.items = items;
        this.inventories = inventories;
        this.transactions = transactions;
        this.statisticsRepository = statisticsRepository;
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

    @Override
    @NotNull
    public ProductsStatisticsResponse getProductStatistics() {
        return transactions.execute(() -> {
            ProductsStatisticsResponse result = analyticsRepository().getProductStatistics();
            BigDecimal average = result.averagePrice();
            return new ProductsStatisticsResponse(result.total(), result.active(), result.archived(), result.withTags(),
                    average == null ? null : average.setScale(2, RoundingMode.HALF_UP));
        });
    }

    @Override
    @NotNull
    public List<CategoryProductCountResponse> getProductsByCategory() {
        return transactions.execute(() -> List.copyOf(analyticsRepository().getProductsByCategory()));
    }

    @Override
    @NotNull
    public List<OrderStatusCountResponse> getOrdersByStatus() {
        return transactions.execute(() -> {
            Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
            for (OrderStatusCountResponse group : analyticsRepository().getOrdersByStatus()) {
                counts.put(group.status(), group.count());
            }
            List<OrderStatusCountResponse> result = new ArrayList<>();
            for (OrderStatus status : OrderStatus.values()) {
                result.add(new OrderStatusCountResponse(status, counts.getOrDefault(status, 0L)));
            }
            return List.copyOf(result);
        });
    }

    @Override
    @NotNull
    public List<ProductStockCountResponse> getProductsByStock() {
        return transactions.execute(() -> {
            Map<StockLevel, Long> counts = new EnumMap<>(StockLevel.class);
            for (ProductStockCountResponse group : analyticsRepository().getProductsByStock(LOW_STOCK_THRESHOLD)) {
                counts.put(group.level(), group.count());
            }
            List<ProductStockCountResponse> result = new ArrayList<>();
            for (StockLevel level : StockLevel.values()) {
                result.add(new ProductStockCountResponse(level, counts.getOrDefault(level, 0L)));
            }
            return List.copyOf(result);
        });
    }

    @NotNull
    private StatisticsRepository analyticsRepository() {
        if (statisticsRepository == null) {
            throw new UnsupportedOperationException(
                    "Для новой аналитики передайте StatisticsRepository в конструктор StatisticsServiceImpl.");
        }
        return statisticsRepository;
    }
}
