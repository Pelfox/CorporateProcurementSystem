package com.team.corporate.backend.config;

import com.team.corporate.core.repositories.*;
import com.team.corporate.core.repositories.impls.*;
import com.team.corporate.core.services.*;
import com.team.corporate.core.services.impls.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoreServicesConfiguration {
    @Bean
    Transactions transactions(EntityManagerFactory factory) {
        return new HibernateTransactions(factory.unwrap(SessionFactory.class));
    }

    @Bean
    UsersRepository usersRepository(EntityManagerFactory factory) {
        return new HibernateUsersRepository(factory.unwrap(SessionFactory.class));
    }

    @Bean
    ProductsRepository productsRepository(EntityManagerFactory factory) {
        return new HibernateProductsRepository(factory.unwrap(SessionFactory.class));
    }

    @Bean
    CategoriesRepository categoriesRepository(EntityManagerFactory factory) {
        return new HibernateCategoriesRepository(factory.unwrap(SessionFactory.class));
    }

    @Bean
    WarehousesRepository warehousesRepository(EntityManagerFactory factory) {
        return new HibernateWarehousesRepository(factory.unwrap(SessionFactory.class));
    }

    @Bean
    InventoriesRepository inventoriesRepository(EntityManagerFactory factory) {
        return new HibernateInventoriesRepository(factory.unwrap(SessionFactory.class));
    }

    @Bean
    OrdersRepository ordersRepository(EntityManagerFactory factory) {
        return new HibernateOrdersRepository(factory.unwrap(SessionFactory.class));
    }

    @Bean
    OrderItemsRepository orderItemsRepository(EntityManagerFactory factory) {
        return new HibernateOrderItemsRepository(factory.unwrap(SessionFactory.class));
    }

    @Bean
    AuditLogsRepository auditLogsRepository(EntityManagerFactory factory) {
        return new HibernateAuditLogsRepository(factory.unwrap(SessionFactory.class));
    }

    @Bean
    OrderStock stockService(ProductsRepository products, InventoriesRepository inventories,
                            OrderItemsRepository items, Transactions transactions) {
        return new OrderStock(products, inventories, items, transactions);
    }

    @Bean
    UsersService usersService(UsersRepository users) {
        return new UsersServiceImpl(users);
    }

    @Bean
    CategoriesService categoriesService(CategoriesRepository categories, ProductsRepository products,
                                        Transactions transactions) {
        return new CategoriesServiceImpl(categories, products, transactions);
    }

    @Bean
    ProductsService productsService(ProductsRepository products, CategoriesRepository categories,
                                    OrderItemsRepository items, InventoriesRepository inventories,
                                    Transactions transactions) {
        return new ProductsServiceImpl(products, categories, items, inventories, transactions);
    }

    @Bean
    WarehousesService warehousesService(WarehousesRepository warehouses) {
        return new WarehousesServiceImpl(warehouses);
    }

    @Bean
    InventoriesService inventoriesService(ProductsRepository products, WarehousesRepository warehouses,
                                          InventoriesRepository inventories, OrderStock stock,
                                          Transactions transactions) {
        return new InventoriesServiceImpl(products, warehouses, inventories, stock, transactions);
    }

    @Bean
    OrdersService ordersService(OrdersRepository orders, UsersRepository users, ProductsRepository products,
                                OrderItemsRepository items, AuditLogsRepository audit,
                                Transactions transactions, OrderStock stock) {
        return new OrdersServiceImpl(orders, users, products, items, audit, transactions, stock);
    }

    @Bean
    OrderItemsService orderItemsService(OrdersRepository orders, ProductsRepository products,
                                        OrderItemsRepository items, Transactions transactions, OrderStock stock) {
        return new OrderItemsServiceImpl(orders, products, items, transactions, stock);
    }

    @Bean
    AuditLogsService auditLogsService(UsersRepository users, OrdersRepository orders, AuditLogsRepository audit) {
        return new AuditLogsServiceImpl(users, orders, audit);
    }

    @Bean
    StatisticsService statisticsService(UsersRepository users, ProductsRepository products,
                                        CategoriesRepository categories, WarehousesRepository warehouses,
                                        OrdersRepository orders, OrderItemsRepository items,
                                        InventoriesRepository inventories, Transactions transactions) {
        return new StatisticsServiceImpl(users, products, categories, warehouses, orders, items,
                inventories, transactions);
    }

    @Bean
    CsvExportService csvExportService(UsersRepository users, CategoriesRepository categories,
                                      ProductsRepository products, WarehousesRepository warehouses,
                                      InventoriesRepository inventories, OrdersRepository orders,
                                      OrderItemsRepository items, AuditLogsRepository audit,
                                      Transactions transactions) {
        return new CsvExportServiceImpl(users, categories, products, warehouses, inventories,
                orders, items, audit, transactions);
    }
}
