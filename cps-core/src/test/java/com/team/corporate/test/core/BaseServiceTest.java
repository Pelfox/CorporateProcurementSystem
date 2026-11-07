package com.team.corporate.test.core;

import com.team.corporate.core.config.FileConfigurationProvider;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.repositories.*;
import com.team.corporate.core.repositories.impls.*;
import com.team.corporate.core.services.*;
import com.team.corporate.core.services.impls.*;
import com.team.corporate.core.utils.HibernateFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;

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
    protected OrderStock stock;
    protected OrderItemsService orderItemsService;
    protected InventoriesService inventoriesService;
    protected WarehousesService warehousesService;
    protected AuditLogsService auditLogsService;

    private static final String TEST_PASSWORD_HASH =
            "$2b$12$0eaPaGYn/83Y3/KcVSPSFOtaK2q4cCgsC0tMkZ83cY.1iGOiA3ePC";

    protected static final Instant TEST_PASSWORD_EXPIRES_AT =
            Instant.parse("2035-01-01T00:00:00Z");

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

    protected User createTestUser(String username, UserRole role) {
        return usersService.createUser(
                username,
                TEST_PASSWORD_HASH,
                TEST_PASSWORD_EXPIRES_AT,
                role
        );
    }
}