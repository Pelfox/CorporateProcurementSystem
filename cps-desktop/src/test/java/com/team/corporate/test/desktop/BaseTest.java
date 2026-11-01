package com.team.corporate.test.desktop;

import com.team.corporate.core.repositories.*;
import com.team.corporate.core.repositories.impls.HibernateTransactions;
import com.team.corporate.core.services.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    protected static SessionFactory sessionFactory;
    protected static HibernateTransactions transactions;
    private static TestDatabase database;

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
        database = TestDatabase.open();
        sessionFactory = database.sessionFactory;
        transactions = database.transactions;
    }

    @AfterAll
    static void closeSessionFactory() {
        if (sessionFactory != null) {
            database.close();
        }
    }

    @BeforeEach
    void setUp() {
        usersRepository = database.usersRepository;
        productsRepository = database.productsRepository;
        categoriesRepository = database.categoriesRepository;
        ordersRepository = database.ordersRepository;
        orderItemsRepository = database.orderItemsRepository;
        inventoriesRepository = database.inventoriesRepository;
        warehousesRepository = database.warehousesRepository;
        auditLogsRepository = database.auditLogsRepository;
        usersService = database.usersService;
        productsService = database.productsService;
        categoriesService = database.categoriesService;
        ordersService = database.ordersService;
        orderItemsService = database.orderItemsService;
        inventoriesService = database.inventoriesService;
        warehousesService = database.warehousesService;
        auditLogsService = database.auditLogsService;
        database.clean();
    }

    protected final void seedData() {
        database.seedData();
    }
}