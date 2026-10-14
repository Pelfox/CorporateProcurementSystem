package com.team.corporate;

import com.team.corporate.repositories.*;
import com.team.corporate.repositories.impls.HibernateTransactions;
import com.team.corporate.services.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        fail("Stage '" + titleRegex + "' не появился");
    }

    protected final void assertDialogOpened(String titleRegex) {
        waitForStage(titleRegex);
        assertTrue(listTargetWindows().stream()
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
        assertTrue(found, "Диалог с элементом " + selector + " не открыт");
    }

    protected final Future<Void> clickAndWaitForDialog(String buttonSelector, String titleRegex) {
        Future<Void> click = WaitForAsyncUtils.async(() -> clickOn(buttonSelector));
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
            fail("Модальный обработчик не завершился после закрытия диалога", exception);
        }
    }

    protected final void awaitDialogAction(Future<Void> dialogAction) {
        try {
            dialogAction.get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            fail("Модальный обработчик не завершился", exception);
        }
    }

    protected final void assertTableContainsText(String tableSelector, String expectedText) {
        boolean found = lookup(tableSelector + " .table-cell")
                .queryAllAs(Labeled.class)
                .stream()
                .anyMatch(cell -> expectedText.equals(cell.getText()));
        assertTrue(found, "Таблица " + tableSelector
                + " не содержит значение: " + expectedText);
    }

    protected final void assertTableDoesNotContainText(String tableSelector, String unexpectedText) {
        boolean found = lookup(tableSelector + " .table-cell")
                .queryAllAs(Labeled.class)
                .stream()
                .anyMatch(cell -> unexpectedText.equals(cell.getText()));
        assertTrue(!found, "Таблица " + tableSelector
                + " содержит удалённое значение: " + unexpectedText);
    }

    protected final void assertVisible(String selector) {
        Node node = lookup(selector).query();
        assertTrue(node.isVisible(), "Элемент " + selector + " не отображается");
    }

    protected final void assertDialogClosed(String titleRegex) {
        WaitForAsyncUtils.waitForFxEvents();
        boolean dialogStillOpen = listTargetWindows().stream()
                .filter(window -> window instanceof Stage)
                .map(window -> ((Stage) window).getTitle())
                .anyMatch(title -> title.matches(".*" + titleRegex + ".*"));
        assertTrue(!dialogStillOpen,
                "Диалог с заголовком '" + titleRegex + "' всё ещё открыт");
    }


}
