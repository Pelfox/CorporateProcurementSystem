package com.team.corporate;

import com.team.corporate.repositories.*;
import com.team.corporate.repositories.impls.HibernateTransactions;
import com.team.corporate.services.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public abstract class BaseUITest extends ApplicationTest {
    protected static SessionFactory sessionFactory;
    protected static HibernateTransactions transactions;
    private static TestDatabase database;

    protected UsersRepository usersRepository;
    protected ProductsRepository productsRepository;
    protected CategoriesRepository categoriesRepository;
    protected OrdersRepository ordersRepository;
    protected OrderItemsRepository orderItemsRepository;
    protected InventoriesRepository inventoriesRepository;
    protected WarehousesRepository warehousesRepository;
    protected AuditLogsRepository auditLogsRepository;

    protected UsersService usersService;
    protected ProductsService productsService;
    protected CategoriesService categoriesService;
    protected OrdersService ordersService;
    protected OrderItemsService orderItemsService;
    protected InventoriesService inventoriesService;
    protected WarehousesService warehousesService;
    protected AuditLogsService auditLogsService;

    @BeforeAll
    static void initDatabase() {
        database = TestDatabase.open();
        sessionFactory = database.sessionFactory;
        transactions = database.transactions;
    }

    @AfterAll
    static void closeDatabase() {
        if (database != null) {
            database.close();
        }
    }

    @BeforeEach
    void setUpDatabase() {
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

    protected void seedData() {
        database.seedData();
    }

    protected String viewResource() {
        return null;
    }

    protected final Parent loadView(String resourcePath) throws IOException {
        URL resource = Objects.requireNonNull(
                getClass().getResource(resourcePath), "FXML resource not found: " + resourcePath);
        return FXMLLoader.load(resource);
    }

    @Override
    public void start(Stage stage) throws IOException {
        String resourcePath = viewResource();
        Parent root = resourcePath == null ? new StackPane() : loadView(resourcePath);
        stage.setScene(new Scene(root));
        stage.show();
    }
}
