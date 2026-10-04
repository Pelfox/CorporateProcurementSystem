package com.team.corporate;

import com.team.corporate.config.FileConfigurationProvider;
import com.team.corporate.repositories.*;
import com.team.corporate.repositories.impls.*;
import com.team.corporate.services.*;
import com.team.corporate.services.impls.*;
import com.team.corporate.utils.HibernateFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseServiceTest {

    protected static SessionFactory sessionFactory;
    protected static HibernateTransactions transactions;

    // Repositories
    protected UsersRepository usersRepository;
    protected ProductsRepository productsRepository;
    protected CategoriesRepository categoriesRepository;
    protected OrdersRepository ordersRepository;
    protected OrderItemsRepository orderItemsRepository;
    protected InventoriesRepository inventoriesRepository;
    protected WarehousesRepository warehousesRepository;
    protected AuditLogsRepository auditLogsRepository;

    // Services
    protected UsersService usersService;
    protected ProductsService productsService;
    protected CategoriesService categoriesService;
    protected OrdersService ordersService;
    protected OrderItemsService orderItemsService;
    protected InventoriesService inventoriesService;
    protected WarehousesService warehousesService;
    protected AuditLogsService auditLogsService;

    @BeforeAll
    static void initSessionFactory() {
        var config = new FileConfigurationProvider().getConfiguration();
        sessionFactory = HibernateFactory.createSessionFactory(config);
        transactions = new HibernateTransactions(sessionFactory);
    }

    @AfterAll
    static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        usersRepository = new HibernateUsersRepository(sessionFactory);
        productsRepository = new HibernateProductsRepository(sessionFactory);
        categoriesRepository = new HibernateCategoriesRepository(sessionFactory);
        ordersRepository = new HibernateOrdersRepository(sessionFactory);
        orderItemsRepository = new HibernateOrderItemsRepository(sessionFactory);
        inventoriesRepository = new HibernateInventoriesRepository(sessionFactory);
        warehousesRepository = new HibernateWarehousesRepository(sessionFactory);
        auditLogsRepository = new HibernateAuditLogsRepository(sessionFactory);

        usersService = new UsersServiceImpl(usersRepository);
        productsService = new ProductsServiceImpl(
                productsRepository, categoriesRepository, orderItemsRepository, inventoriesRepository, transactions);
        categoriesService = new CategoriesServiceImpl(categoriesRepository, productsRepository, transactions);
        ordersService = new OrdersServiceImpl(
                ordersRepository, usersRepository, productsRepository, orderItemsRepository, auditLogsRepository, transactions);
        orderItemsService = new OrderItemsServiceImpl(ordersRepository, productsRepository, orderItemsRepository);
        inventoriesService = new InventoriesServiceImpl(productsRepository, warehousesRepository, inventoriesRepository);
        warehousesService = new WarehousesServiceImpl(warehousesRepository);
        auditLogsService = new AuditLogsServiceImpl(usersRepository, ordersRepository, auditLogsRepository);

        cleanDatabase();
    }

    private void cleanDatabase() {
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
}