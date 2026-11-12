package com.team.corporate.backend.config;

import com.team.corporate.core.repositories.AuditLogsRepository;
import com.team.corporate.core.repositories.CategoriesRepository;
import com.team.corporate.core.repositories.InventoriesRepository;
import com.team.corporate.core.repositories.OrderItemsRepository;
import com.team.corporate.core.repositories.OrdersRepository;
import com.team.corporate.core.repositories.ProductsRepository;
import com.team.corporate.core.repositories.SessionOperations;
import com.team.corporate.core.repositories.Transactions;
import com.team.corporate.core.repositories.UsersRepository;
import com.team.corporate.core.repositories.WarehousesRepository;
import com.team.corporate.core.repositories.impls.HibernateAuditLogsRepository;
import com.team.corporate.core.repositories.impls.HibernateCategoriesRepository;
import com.team.corporate.core.repositories.impls.HibernateInventoriesRepository;
import com.team.corporate.core.repositories.impls.HibernateOrderItemsRepository;
import com.team.corporate.core.repositories.impls.HibernateOrdersRepository;
import com.team.corporate.core.repositories.impls.HibernateProductsRepository;
import com.team.corporate.core.repositories.impls.HibernateWarehousesRepository;
import com.team.corporate.core.services.AuditLogsService;
import com.team.corporate.core.services.CategoriesService;
import com.team.corporate.core.services.CsvExportService;
import com.team.corporate.core.services.InventoriesService;
import com.team.corporate.core.services.OrderItemsService;
import com.team.corporate.core.services.OrdersService;
import com.team.corporate.core.services.ProductsService;
import com.team.corporate.core.services.StatisticsService;
import com.team.corporate.core.services.UsersService;
import com.team.corporate.core.services.WarehousesService;
import com.team.corporate.core.services.impls.AuditLogsServiceImpl;
import com.team.corporate.core.services.impls.CategoriesServiceImpl;
import com.team.corporate.core.services.impls.CsvExportServiceImpl;
import com.team.corporate.core.services.impls.InventoriesServiceImpl;
import com.team.corporate.core.services.impls.OrderItemsServiceImpl;
import com.team.corporate.core.services.impls.OrdersServiceImpl;
import com.team.corporate.core.services.impls.OrderStock;
import com.team.corporate.core.services.impls.ProductsServiceImpl;
import com.team.corporate.core.services.impls.StatisticsServiceImpl;
import com.team.corporate.core.services.impls.UsersServiceImpl;
import com.team.corporate.core.services.impls.WarehousesServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoreServicesConfiguration {
    @Bean
    ProductsRepository productsRepository(SessionOperations sessions) {
        return new HibernateProductsRepository(sessions);
    }

    @Bean
    CategoriesRepository categoriesRepository(SessionOperations sessions) {
        return new HibernateCategoriesRepository(sessions);
    }

    @Bean
    WarehousesRepository warehousesRepository(SessionOperations sessions) {
        return new HibernateWarehousesRepository(sessions);
    }

    @Bean
    InventoriesRepository inventoriesRepository(SessionOperations sessions) {
        return new HibernateInventoriesRepository(sessions);
    }

    @Bean
    OrdersRepository ordersRepository(SessionOperations sessions) {
        return new HibernateOrdersRepository(sessions);
    }

    @Bean
    OrderItemsRepository orderItemsRepository(SessionOperations sessions) {
        return new HibernateOrderItemsRepository(sessions);
    }

    @Bean
    AuditLogsRepository auditLogsRepository(SessionOperations sessions) {
        return new HibernateAuditLogsRepository(sessions);
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
