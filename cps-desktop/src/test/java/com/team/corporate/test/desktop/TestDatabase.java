package com.team.corporate.test.desktop;

import com.team.corporate.core.config.FileConfigurationProvider;
import com.team.corporate.core.repositories.*;
import com.team.corporate.core.repositories.impls.*;
import com.team.corporate.core.services.*;
import com.team.corporate.core.services.impls.*;
import com.team.corporate.core.utils.DataInitializer;
import com.team.corporate.core.utils.HibernateFactory;
import org.hibernate.SessionFactory;

final class TestDatabase implements AutoCloseable {
    final SessionFactory sessionFactory;
    final HibernateTransactions transactions;

    final UsersRepository usersRepository;
    final ProductsRepository productsRepository;
    final CategoriesRepository categoriesRepository;
    final OrdersRepository ordersRepository;
    final OrderItemsRepository orderItemsRepository;
    final InventoriesRepository inventoriesRepository;
    final WarehousesRepository warehousesRepository;
    final AuditLogsRepository auditLogsRepository;

    final OrderStock stock;

    final UsersService usersService;
    final ProductsService productsService;
    final CategoriesService categoriesService;
    final OrdersService ordersService;
    final OrderItemsService orderItemsService;
    final InventoriesService inventoriesService;
    final WarehousesService warehousesService;
    final AuditLogsService auditLogsService;

    private TestDatabase() {
        sessionFactory = HibernateFactory.createSessionFactory(new FileConfigurationProvider().getConfiguration());
        transactions = new HibernateTransactions(sessionFactory);

        usersRepository = new HibernateUsersRepository(sessionFactory);
        productsRepository = new HibernateProductsRepository(sessionFactory);
        categoriesRepository = new HibernateCategoriesRepository(sessionFactory);
        ordersRepository = new HibernateOrdersRepository(sessionFactory);
        orderItemsRepository = new HibernateOrderItemsRepository(sessionFactory);
        inventoriesRepository = new HibernateInventoriesRepository(sessionFactory);
        warehousesRepository = new HibernateWarehousesRepository(sessionFactory);
        auditLogsRepository = new HibernateAuditLogsRepository(sessionFactory);

        stock = new OrderStock(productsRepository, inventoriesRepository, orderItemsRepository, transactions);

        usersService = new UsersServiceImpl(usersRepository);
        productsService = new ProductsServiceImpl(
                productsRepository, categoriesRepository, orderItemsRepository, inventoriesRepository, transactions);
        categoriesService = new CategoriesServiceImpl(categoriesRepository, productsRepository, transactions);
        ordersService = new OrdersServiceImpl(
                ordersRepository, usersRepository, productsRepository, orderItemsRepository, auditLogsRepository, transactions, stock);
        orderItemsService = new OrderItemsServiceImpl(ordersRepository, productsRepository, orderItemsRepository, transactions, stock);
        inventoriesService = new InventoriesServiceImpl(productsRepository, warehousesRepository, inventoriesRepository, stock, transactions);
        warehousesService = new WarehousesServiceImpl(warehousesRepository);
        auditLogsService = new AuditLogsServiceImpl(usersRepository, ordersRepository, auditLogsRepository);
    }

    static TestDatabase open() {
        return new TestDatabase();
    }

    void clean() {
        sessionFactory.inTransaction(session -> {
            session.createMutationQuery("DELETE FROM AuditLog").executeUpdate();
            session.createMutationQuery("DELETE FROM OrderItem").executeUpdate();
            session.createMutationQuery("DELETE FROM Order").executeUpdate();
            session.createMutationQuery("DELETE FROM Inventory").executeUpdate();
            session.createMutationQuery("DELETE FROM Product").executeUpdate();
            session.createMutationQuery("DELETE FROM Category").executeUpdate();
            session.createMutationQuery("DELETE FROM User").executeUpdate();
            session.createMutationQuery("DELETE FROM Warehouse").executeUpdate();
        });
    }

    void seedData() {
        transactions.execute(() -> new DataInitializer(
                usersService, categoriesService, productsService, ordersService, warehousesService, inventoriesService).run());
    }

    @Override
    public void close() {
        sessionFactory.close();
    }
}
