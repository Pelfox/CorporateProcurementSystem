package com.team.corporate.test.desktop;

import com.team.corporate.core.config.ApplicationConfiguration;
import com.team.corporate.core.config.ApplicationServices;
import com.team.corporate.core.repositories.*;
import com.team.corporate.core.repositories.impls.HibernateTransactions;
import com.team.corporate.core.services.*;
import com.team.corporate.desktop.controllers.ControllerFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

public abstract class BaseUITest extends ApplicationTest {
    protected static SessionFactory sessionFactory;
    protected static HibernateTransactions transactions;
    private static TestDatabase database;
    private static ApplicationServices services;

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
        services = ApplicationServices.open(servicesConfiguration());
    }

    // Контроллерам нужны сервисы из ControllerFactory; схему создаёт TestDatabase, поэтому DDL здесь отключён.
    private static ApplicationConfiguration servicesConfiguration() {
        try (InputStream input = BaseUITest.class.getResourceAsStream("/application.properties")) {
            Properties properties = new Properties();
            properties.load(Objects.requireNonNull(input, "application.properties not found"));
            properties.setProperty("hibernate.ddl.auto", "none");
            return ApplicationConfiguration.fromProperties(properties);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @AfterAll
    static void closeDatabase() {
        if (services != null) {
            services.close();
        }
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
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setControllerFactory(new ControllerFactory(() -> services));
        return loader.load();
    }

    @Override
    public void start(Stage stage) throws IOException {
        String resourcePath = viewResource();
        Parent root = resourcePath == null ? new StackPane() : loadView(resourcePath);
        stage.setScene(new Scene(root));
        stage.show();
    }

    public void waitForStage(String titleRegex) {
        // Ждём, пока появится Stage с нужным заголовком
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            boolean found = listTargetWindows().stream()
                    .anyMatch(w -> w instanceof javafx.stage.Stage
                            && ((javafx.stage.Stage) w).getTitle().matches(".*" + titleRegex + ".*"));
            if (found) return;
            sleep(50, TimeUnit.MILLISECONDS);
        }
        Assertions.fail("Stage '" + titleRegex + "' не появился");
    }

    protected final void assertDialogOpened(String titleRegex) {
        waitForStage(titleRegex);
        Assertions.assertTrue(listTargetWindows().stream()
                        .filter(window -> window instanceof Stage)
                        .map(window -> ((Stage) window).getTitle())
                        .anyMatch(title -> title.matches(".*" + titleRegex + ".*")),
                "Диалог с заголовком '" + titleRegex + "' не открыт");
    }

    protected final void assertDialogContains(String selector) {
        boolean found = listTargetWindows().stream()
                .filter(window -> window instanceof Stage)
                .map(window -> ((Stage) window).getScene())
                .filter(Objects::nonNull)
                .map(Scene::getRoot)
                .map(root -> root.lookup(selector))
                .anyMatch(Objects::nonNull);
        Assertions.assertTrue(found, "Диалог с элементом " + selector + " не открыт");
    }

    protected final Future<Void> clickAndWaitForDialog(String buttonSelector, String titleRegex) {
        Future<Void> click = WaitForAsyncUtils.async(() -> {
            clickOn(buttonSelector);
            return null;
        });
        waitForStage(titleRegex);
        assertDialogOpened(titleRegex);
        return click;
    }

    protected final void finishDialogInteraction(Future<Void> dialogAction,
                                                 String closeButtonSelector,
                                                 String titleRegex) {
        clickOn(closeButtonSelector);
        WaitForAsyncUtils.waitForFxEvents();
        assertDialogClosed(titleRegex);
        try {
            dialogAction.get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            Assertions.fail("Модальный обработчик не завершился после закрытия диалога", exception);
        }
    }

    protected final void awaitDialogAction(Future<Void> dialogAction) {
        try {
            dialogAction.get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            Assertions.fail("Модальный обработчик не завершился", exception);
        }
    }

    protected final void assertTableContainsText(String tableSelector, String expectedText) {
        boolean found = lookup(tableSelector + " .table-cell")
                .queryAllAs(Labeled.class)
                .stream()
                .anyMatch(cell -> expectedText.equals(cell.getText()));
        Assertions.assertTrue(found, "Таблица " + tableSelector
                + " не содержит значение: " + expectedText);
    }

    protected final void assertTableDoesNotContainText(String tableSelector, String unexpectedText) {
        boolean found = lookup(tableSelector + " .table-cell")
                .queryAllAs(Labeled.class)
                .stream()
                .anyMatch(cell -> unexpectedText.equals(cell.getText()));
        Assertions.assertTrue(!found, "Таблица " + tableSelector
                + " содержит удалённое значение: " + unexpectedText);
    }

    protected final void assertVisible(String selector) {
        Node node = lookup(selector).query();
        Assertions.assertTrue(node.isVisible(), "Элемент " + selector + " не отображается");
    }

    protected final void assertDialogClosed(String titleRegex) {
        WaitForAsyncUtils.waitForFxEvents();
        boolean dialogStillOpen = listTargetWindows().stream()
                .filter(window -> window instanceof Stage)
                .map(window -> ((Stage) window).getTitle())
                .anyMatch(title -> title.matches(".*" + titleRegex + ".*"));
        Assertions.assertTrue(!dialogStillOpen,
                "Диалог с заголовком '" + titleRegex + "' всё ещё открыт");
    }


}
