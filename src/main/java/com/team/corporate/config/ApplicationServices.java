package com.team.corporate.config;

import com.team.corporate.repositories.impls.*;
import com.team.corporate.services.*;
import com.team.corporate.services.impls.*;
import com.team.corporate.utils.DataInitializer;
import com.team.corporate.utils.HibernateFactory;
import org.hibernate.SessionFactory;

public final class ApplicationServices implements AutoCloseable {
    private final SessionFactory factory;

    private final UsersService users;
    private final ProductsService products;
    private final CategoriesService categories;
    private final OrdersService orders;
    private final OrderItemsService items;
    private final InventoriesService inventories;
    private final WarehousesService warehouses;
    private final StatisticsService statistics;
    private final CsvExportService csvExport;

    public static ApplicationServices open(ApplicationConfiguration configuration) {
        SessionFactory sessionFactory = HibernateFactory.createSessionFactory(configuration);
        try {
            return new ApplicationServices(sessionFactory);
        } catch (Exception | Error e) {
            sessionFactory.close();
            throw e;
        }
    }

    private ApplicationServices(SessionFactory sessionFactory) {
        this.factory = sessionFactory;

        var transactions = new HibernateTransactions(factory);
        var usersRepository = new HibernateUsersRepository(factory);
        var productsRepository = new HibernateProductsRepository(factory);
        var categoriesRepository = new HibernateCategoriesRepository(factory);
        var ordersRepository = new HibernateOrdersRepository(factory);
        var itemsRepository = new HibernateOrderItemsRepository(factory);
        var inventoriesRepository = new HibernateInventoriesRepository(factory);
        var warehousesRepository = new HibernateWarehousesRepository(factory);
        var auditLogsRepository = new HibernateAuditLogsRepository(factory);

        users = new UsersServiceImpl(usersRepository);
        products = new ProductsServiceImpl(productsRepository, categoriesRepository,
                itemsRepository, inventoriesRepository, transactions);
        categories = new CategoriesServiceImpl(categoriesRepository, productsRepository, transactions);
        orders = new OrdersServiceImpl(ordersRepository, usersRepository, productsRepository,
                itemsRepository, auditLogsRepository, transactions);
        items = new OrderItemsServiceImpl(ordersRepository, productsRepository, itemsRepository);
        inventories = new InventoriesServiceImpl(productsRepository, warehousesRepository, inventoriesRepository);
        warehouses = new WarehousesServiceImpl(warehousesRepository);
        statistics = new StatisticsServiceImpl(usersRepository, productsRepository, categoriesRepository,
                warehousesRepository, ordersRepository, itemsRepository, inventoriesRepository, transactions);
        csvExport = new CsvExportServiceImpl(usersRepository, categoriesRepository, productsRepository,
                warehousesRepository, inventoriesRepository, ordersRepository, itemsRepository,
                auditLogsRepository, transactions);

        transactions.execute(() -> new DataInitializer(users, categories, products, orders).run());
    }

    public UsersService users() { return users; }
    public ProductsService products() { return products; }
    public CategoriesService categories() { return categories; }
    public OrdersService orders() { return orders; }
    public OrderItemsService items() { return items; }
    public InventoriesService inventories() { return inventories; }
    public WarehousesService warehouses() { return warehouses; }
    public StatisticsService statistics() { return statistics; }
    public CsvExportService csvExport() { return csvExport; }

    @Override
    public void close() {
        factory.close();
    }
}