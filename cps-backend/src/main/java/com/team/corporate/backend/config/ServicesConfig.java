package com.team.corporate.backend.config;

import com.team.corporate.core.repositories.*;
import com.team.corporate.core.services.*;
import com.team.corporate.core.services.impls.*;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;

@Configuration(proxyBeanMethods = false)
public class ServicesConfig {
    @Bean
    public Transactions transactions(PlatformTransactionManager manager) {
        var template = new TransactionTemplate(manager);

        return new Transactions() {
            @Override
            public <T> T execute(@NonNull Supplier<T> action) {
                Objects.requireNonNull(action, "action must not be null");
                return template.execute(_ -> action.get());
            }
        };
    }

    @Bean
    public UsersService usersService(UsersRepository users) {
        return new UsersServiceImpl(users);
    }

    @Bean
    public WarehousesService warehousesService(WarehousesRepository warehouses) {
        return new WarehousesServiceImpl(warehouses);
    }

    @Bean
    public CategoriesService categoriesService(
            CategoriesRepository categories,
            ProductsRepository products,
            Transactions transactions
    ) {
        return new CategoriesServiceImpl(categories, products, transactions);
    }

    @Bean
    public ProductsService productsService(
            ProductsRepository products,
            CategoriesRepository categories,
            OrderItemsRepository items,
            InventoriesRepository inventories,
            Transactions transactions
    ) {
        return new ProductsServiceImpl(
                products, categories, items, inventories, transactions
        );
    }

    @Bean
    public OrderStock orderStock(
            ProductsRepository products,
            InventoriesRepository inventories,
            OrderItemsRepository items,
            Transactions transactions
    ) {
        return new OrderStock(products, inventories, items, transactions);
    }

    @Bean
    public OrdersService ordersService(
            OrdersRepository orders,
            UsersRepository users,
            ProductsRepository products,
            OrderItemsRepository items,
            AuditLogsRepository auditLogs,
            Transactions transactions,
            OrderStock stock
    ) {
        return new OrdersServiceImpl(
                orders, users, products, items, auditLogs, transactions, stock
        );
    }

    @Bean
    public OrderItemsService orderItemsService(
            OrdersRepository orders,
            ProductsRepository products,
            OrderItemsRepository items,
            Transactions transactions,
            OrderStock stock
    ) {
        return new OrderItemsServiceImpl(
                orders, products, items, transactions, stock
        );
    }

    @Bean
    public InventoriesService inventoriesService(
            ProductsRepository products,
            WarehousesRepository warehouses,
            InventoriesRepository inventories,
            OrderStock stock,
            Transactions transactions
    ) {
        return new InventoriesServiceImpl(
                products, warehouses, inventories, stock, transactions
        );
    }

    @Bean
    public AuditLogsService auditLogsService(
            UsersRepository users,
            OrdersRepository orders,
            AuditLogsRepository auditLogs
    ) {
        return new AuditLogsServiceImpl(users, orders, auditLogs);
    }

    @Bean
    public StatisticsService statisticsService(
            UsersRepository users,
            ProductsRepository products,
            CategoriesRepository categories,
            WarehousesRepository warehouses,
            OrdersRepository orders,
            OrderItemsRepository items,
            InventoriesRepository inventories,
            Transactions transactions,
            StatisticsRepository statistics
    ) {
        return new StatisticsServiceImpl(
                users, products, categories, warehouses,
                orders, items, inventories, transactions, statistics
        );
    }

    @Bean
    public CsvExportService csvExportService(
            UsersRepository users,
            CategoriesRepository categories,
            ProductsRepository products,
            WarehousesRepository warehouses,
            InventoriesRepository inventories,
            OrdersRepository orders,
            OrderItemsRepository items,
            AuditLogsRepository auditLogs,
            Transactions transactions
    ) {
        return new CsvExportServiceImpl(
                users, categories, products, warehouses,
                inventories, orders, items, auditLogs, transactions
        );
    }

    @Bean
    public CsvImportService csvImportService(
            UsersRepository users,
            CategoriesRepository categories,
            ProductsRepository products,
            Transactions transactions,
            Clock clock
    ) {
        return new CsvImportServiceImpl(
                users, categories, products, transactions, clock
        );
    }
}
