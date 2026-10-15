package com.team.corporate;

import com.team.corporate.config.ApplicationConfiguration;
import com.team.corporate.config.ApplicationServices;
import com.team.corporate.controllers.ControllerFactory;
import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.DialogService;
import com.team.corporate.controllers.dialog.ViewType;
import com.team.corporate.controllers.modals.OrderEditorController;
import com.team.corporate.controllers.navigation.NavigationService;
import com.team.corporate.controllers.navigation.Page;
import com.team.corporate.controllers.requests.*;
import com.team.corporate.controllers.session.UserSession;
import com.team.corporate.entities.Order;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.entities.UserRole;
import com.team.corporate.services.StockService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Run with ./gradlew uiValidation. Uses only its own in-memory database.
 */
public final class UiValidationSmoke {
    private static final Set<String> checkedFields = new TreeSet<>();
    private static int checks;
    private static Stage stage;
    private static ApplicationServices app;
    private static ControllerFactory factory;
    private static UserSession manager;

    static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:h2:mem:ui_validation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        properties.setProperty("db.username", "sa");
        properties.setProperty("db.password", "");
        properties.setProperty("hibernate.ddl.auto", "create-drop");
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);
        try (ApplicationServices services = ApplicationServices.open(ApplicationConfiguration.fromProperties(properties))) {
            app = services;
            factory = new ControllerFactory(() -> app);
            manager = UserSession.of(app.users().login("matvey_rat").orElseThrow());
            Platform.startup(() -> Platform.runLater(() -> {
                Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> failure.compareAndSet(null, error));
                try {
                    stage = new Stage();
                    stage.setTitle("Проверка валидации — тестовая база");
                    verify();
                } catch (Throwable error) {
                    failure.compareAndSet(null, error);
                } finally {
                    List.copyOf(Window.getWindows()).forEach(Window::hide);
                    Platform.exit();
                    finished.countDown();
                }
            }));
            if (!finished.await(60, TimeUnit.SECONDS)) {
                System.err.println("UI validation timed out");
                System.exit(2);
            }
            if (failure.get() != null) throw new AssertionError("UI validation failed", failure.get());
            System.out.println("UI_VALIDATION_PASSED checks=" + checks + ", fields=" + checkedFields.size());
            checkedFields.forEach(System.out::println);
        }
    }

    private static void verify() throws Exception {
        for (String path : List.of("login/LoginView", "products/ProductsView", "categories/CategoriesView",
                "warehouses/WarehousesView", "users/UsersView", "orders/OrdersView", "orders/MyOrdersView")) {
            Form form = load(path, path.startsWith("login/") || path.startsWith("products/")
                    || path.startsWith("categories/") || path.startsWith("warehouses/") ? null : manager);
            inputs(form);
            for (String id : List.of("editBtn", "deleteBtn", "statusBtn", "editWarehouseBtn")) {
                if (form.nodes.get(id) instanceof Button button) invoke(button);
            }
            if (path.equals("orders/MyOrdersView")) dates(form);
        }
        var category = app.categories().getAll().getFirst();
        for (Object request : List.of(CategoryEditorRequest.forCreate(), CategoryEditorRequest.forEdit(category))) {
            Form form = load("categories/CategoryDialog", request);
            inputs(form);
            invalidSave(form, "nameField", Arrays.asList(null, "", " ", "Я".repeat(256), " ".repeat(10_000) + "a"));
        }
        var warehouse = app.warehouses().getAll().getFirst();
        for (Object request : List.of(WarehouseEditorRequest.forCreate(), WarehouseEditorRequest.forEdit(warehouse))) {
            Form form = load("warehouses/WarehouseDialog", request);
            inputs(form);
            text(form, "nameField").setText("Склад");
            invalidSave(form, "locationField", List.of("Я".repeat(256), "Я".repeat(10_000)));
            invalidSave(form, "nameField", List.of("", " ", "Я".repeat(256)));
        }
        var user = app.users().login("ivan_sid").orElseThrow();
        for (Object request : List.of(UserEditorRequest.forCreate(), UserEditorRequest.forEdit(user))) {
            Form form = load("users/UserDialog", request);
            inputs(form);
            combo(form, "roleCombo").setValue(UserRole.USER);
            invalidSave(form, "usernameField", Arrays.asList(null, "", " ", "Я".repeat(256), "Я".repeat(10_000)));
        }
        var product = app.products().getAll().getFirst();
        for (Object request : List.of(ProductEditorRequest.forCreate(), ProductEditorRequest.forEdit(product))) {
            Form form = load("products/ProductDialog", request);
            inputs(form);
            combo(form, "categoryCombo").getSelectionModel().selectFirst();
            text(form, "nameField").setText("Товар");
            text(form, "skuField").setText("VALID-001");
            text(form, "priceField").setText("12,50");
            check(!button(form, "saveBtn").isDisabled(), "Valid product is enabled");
            invalidSave(form, "nameField", List.of("", " ", "Я".repeat(256)));
            invalidSave(form, "skuField", Arrays.asList(null, "", "AB", "SKU--001", "A".repeat(41)));
            invalidSave(form, "priceField", Arrays.asList(null, "", ".", "1,", "0", "-1", "1.234", "100000000",
                    "1e999999999", "NaN", "9".repeat(10_000)));
        }
        Form inventory = load("warehouses/InventoryDialog", null);
        inputs(inventory);
        combo(inventory, "warehouseCombo").getSelectionModel().selectFirst();
        combo(inventory, "productCombo").getSelectionModel().selectFirst();
        invalidSave(inventory, "quantityField", List.of("", "-1", "+1", "1.5", "2147483648", "9".repeat(10_000)));

        Order order = app.orders().getAll().stream().filter(o -> o.getStatus() == OrderStatus.CREATED).findFirst().orElseThrow();
        for (Object request : List.of(OrderEditorRequest.forCreate(manager.userId()),
                OrderEditorRequest.forEdit(order.getUser().getId(), order))) {
            Form form = load("orders/OrderCreateDialog", request);
            inputs(form);
            combo(form, "productCombo").getSelectionModel().selectFirst();
            text(form, "quantityField").setText("1");
            invoke(button(form, "addItemBtn"));
            invalidSave(form, "notesArea", List.of("Я".repeat(256), "Я".repeat(10_000)));
            combo(form, "productCombo").getSelectionModel().selectFirst();
            invalidAction(form, "quantityField", "addItemBtn", List.of("", "0", "-1", "+1", "100001", "2147483648"));
        }
        inputs(load("orders/OrderStatusDialog", new OrderStatusChangeRequest(order.getId(), manager.userId())));
        noteDialog(order);
        failures();
        assertFieldCoverage();
    }

    private static void assertFieldCoverage() throws Exception {
        var xml = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        xml.setNamespaceAware(true);
        xml.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var base = java.nio.file.Path.of("src/main/resources/fxml");
        try (var files = java.nio.file.Files.walk(base)) {
            for (var file : files.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                var elements = xml.newDocumentBuilder().parse(file.toFile()).getElementsByTagName("*");
                for (int i = 0; i < elements.getLength(); i++) {
                    var element = (org.w3c.dom.Element) elements.item(i);
                    if (Set.of("TextField", "TextArea", "ComboBox", "DatePicker").contains(element.getTagName())) {
                        String key = base.relativize(file).toString().replace('\\', '/').replace(".fxml", "")
                                + "#" + element.getAttributeNS("http://javafx.com/fxml/1", "id");
                        check(checkedFields.contains(key), "UI field has no validation test: " + key);
                    }
                }
            }
        }
    }

    private static void inputs(Form form) {
        for (var entry : new TreeMap<>(form.nodes).entrySet()) {
            String id = entry.getKey();
            Object node = entry.getValue();
            if (node instanceof TextInputControl input) {
                checkedFields.add(form.path + "#" + id);
                input.setText(null);
                check(input.getLength() == 0, "Null text is safe: " + id);
                if (id.equals("priceFromField") || id.equals("priceToField")) digits(input);
                else if (id.equals("quantityField"))
                    integer(input, form.path.startsWith("orders/") ? 100_000 : Integer.MAX_VALUE);
                else if (id.equals("priceField")) money(input);
                else if (id.equals("skuField")) sku(input);
                else limited(input);
            } else if (node instanceof ComboBox<?> combo) {
                checkedFields.add(form.path + "#" + id);
                check(!combo.isEditable(), "Combo uses list selection: " + id);
                combo.setValue(null);
                combo.getSelectionModel().selectFirst();
                combo.getSelectionModel().selectLast();
                combo.setValue(null);
            } else if (node instanceof DatePicker picker) {
                checkedFields.add(form.path + "#" + id);
                check(!picker.isEditable() && !picker.getEditor().isEditable(), "Calendar is not editable");
            }
        }
    }

    private static void limited(TextInputControl field) {
        field.setText(null);
        check(field.getLength() == 0, "Null text remains safe");
        field.clear();
        field.setText("Я".repeat(255));
        check(field.getLength() == 255, "Text accepts exactly 255");
        field.appendText("Я");
        check(field.getLength() == 255, "Text rejects 256th character");
        field.selectAll();
        field.replaceSelection("Я".repeat(10_000));
        check(field.getLength() == 255, "Text rejects oversized paste");
        field.setText("Исправлено");
        check(field.getText().equals("Исправлено"), "Text remains editable after rejected paste");
        field.clear();
    }

    private static void digits(TextInputControl field) {
        field.setText("9".repeat(18));
        check(field.getLength() == 18, "Filter accepts 18 digits");
        reject(field, List.of("9".repeat(19), "9".repeat(10_000), "-1", "+1", "1.2", "1,2", "1e5", "abc", "١٢"));
        field.clear();
    }

    private static void integer(TextInputControl field, int maximum) {
        field.setText(String.valueOf(maximum));
        check(field.getText().equals(String.valueOf(maximum)), "Integer upper boundary is accepted");
        reject(field, List.of(String.valueOf((long) maximum + 1), "9".repeat(10_000), "-1", "+1", "1.2", "1e5", "abc", "١٢"));
        field.setText("1");
    }

    private static void money(TextInputControl field) {
        field.clear();
        for (String digit : List.of("1", "2", ",", "5", "0")) field.appendText(digit);
        check(field.getText().equals("12,50"), "Typing money does not reformat partial input");
        field.setText("99999999.99");
        check(field.getText().equals("99999999.99"), "Money maximum accepted");
        reject(field, List.of("100000000", "1.234", "-1", "+1", "NaN", "Infinity", "1e999999999", "9".repeat(10_000)));
        field.setText("12,50");
    }

    private static void sku(TextInputControl field) {
        field.setText("abc-001");
        check(field.getText().equals("ABC-001"), "SKU normalizes case");
        reject(field, List.of("A".repeat(41), "A".repeat(10_000), "абв", "ABC 001", "ABC/001"));
        field.setText("VALID-001");
    }

    private static void reject(TextInputControl field, List<String> values) {
        String original = field.getText();
        for (String value : values) {
            field.selectAll();
            field.replaceSelection(value);
            check(field.getText().equals(original), "Invalid paste rejected (length " + value.length() + ")");
        }
    }

    private static void invalidSave(Form form, String field, List<String> values) {
        invalidAction(form, field, "saveBtn", values);
    }

    private static void invalidAction(Form form, String id, String action, List<String> values) {
        if (action.equals("saveBtn") && !form.nodes.containsKey(action)) action = "createBtn";
        TextInputControl input = text(form, id);
        TextFormatter<?> formatter = input.getTextFormatter();
        String original = input.getText();
        input.setTextFormatter(null); // Deliberately bypass the UI filter to exercise controller validation.
        for (String value : values) {
            input.setText(value);
            check(button(form, action).isDisabled(), "Invalid value disables action: " + form.path + "#" + id);
            invoke(button(form, action)); // Bypass disabled state as well; the handler must check again.
            check(stage.isShowing(), "Invalid direct callback does not close or save form");
        }
        input.setText(original);
        input.setTextFormatter(formatter);
    }

    private static void dates(Form form) {
        DatePicker from = (DatePicker) form.nodes.get("dateFromPicker");
        DatePicker to = (DatePicker) form.nodes.get("dateToPicker");
        from.setValue(LocalDate.MAX);
        to.setValue(LocalDate.MIN);
        check(((Label) form.nodes.get("dateError")).isVisible(), "Inverted date range explains error");
        invoke(button(form, "clearDatesBtn"));
        check(from.getValue() == null && to.getValue() == null, "Date reset clears both values");
        check(from.getEditor().getText().isEmpty() && to.getEditor().getText().isEmpty(), "Date reset clears text");
    }

    private static void noteDialog(Order order) throws Exception {
        Form form = load("orders/OrdersView", UserSession.of(order.getUser()));
        @SuppressWarnings("unchecked") TableView<Order> table = (TableView<Order>) form.nodes.get("ordersTable");
        table.getSelectionModel().select(table.getItems().stream().filter(o -> o.getId().equals(order.getId())).findFirst().orElseThrow());
        AtomicBoolean visited = new AtomicBoolean();
        Platform.runLater(() -> {
            Window dialog = Window.getWindows().stream().filter(w -> w != stage && w.isShowing()).findFirst().orElseThrow();
            DialogPane pane = (DialogPane) dialog.getScene().getRoot();
            TextField editor = (TextField) pane.lookup(".text-field");
            checkedFields.add("orders/OrdersView#notesEditor");
            limited(editor);
            visited.set(true);
            ((Button) pane.lookupButton(ButtonType.CANCEL)).fire();
        });
        invoke(button(form, "editBtn"));
        check(visited.get(), "Notes dialog limits checked");
    }

    private static void failures() throws Exception {
        Parent previous = stage.getScene().getRoot();
        expectAlert(() -> check(new DialogService(factory).showModal(ViewType.EDIT_CATEGORY, stage,
                new CategoryEditorRequest(UUID.randomUUID())).isEmpty(), "Stale selection returns empty result"));
        check(stage.getScene().getRoot() == previous, "Failed modal leaves page intact");
        StackPane content = new StackPane();
        stage.setScene(new Scene(content, 1100, 800));
        NavigationService navigation = new NavigationService(content, type -> {
            if (type.getSimpleName().equals("CatalogController"))
                throw new IllegalStateException("Test database failure");
            return factory.call(type);
        }, manager, page -> {
        });
        navigation.navigate(Page.MY_ORDERS);
        navigation.navigate(Page.SERVICES);
        Parent services = (Parent) content.getChildren().getFirst();
        expectAlert(() -> navigation.navigate(Page.PRODUCTS));
        check(content.getChildren().getFirst() == services, "Failed navigation preserves current page");
        navigation.goBack();
        content.applyCss();
        content.layout();
        check(content.lookup("#welcomeBanner") != null, "Failed navigation preserves history");

        AtomicBoolean fail = new AtomicBoolean();
        StockService stock = editing -> {
            if (fail.get()) throw new IllegalStateException("Test stock read failure");
            return app.stock().getAvailableQuantities(editing);
        };
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/orders/OrderCreateDialog.fxml"));
        loader.setControllerFactory(type -> new OrderEditorController(app.orders(), app.products(), app.items(), stock));
        Parent root = loader.load();
        OrderEditorController controller = loader.getController();
        controller.receiveData(OrderEditorRequest.forCreate(manager.userId()));
        stage.setScene(new Scene(root, 1100, 800));
        Form form = new Form("orders/OrderCreateDialog", loader.getNamespace());
        combo(form, "productCombo").getSelectionModel().selectFirst();
        fail.set(true);
        expectAlert(() -> invoke(button(form, "addItemBtn")));
        check(((TableView<?>) form.nodes.get("itemsTable")).getItems().isEmpty(), "Failed stock check does not add item");
    }

    private static void expectAlert(Runnable action) {
        AtomicBoolean shown = new AtomicBoolean();
        Platform.runLater(() -> {
            Window alert = Window.getWindows().stream().filter(w -> w != stage && w.isShowing()).findFirst().orElseThrow();
            check(alert.getScene().getRoot() instanceof DialogPane, "Failure shown as a dialog");
            shown.set(true);
            alert.hide();
        });
        action.run();
        check(shown.get(), "Failure is handled without an uncaught exception");
    }

    @SuppressWarnings("unchecked")
    private static Form load(String path, Object payload) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/" + path + ".fxml"));
        loader.setControllerFactory(factory);
        Parent root = loader.load();
        if (payload != null && loader.getController() instanceof DataReceiver<?> receiver) {
            ((DataReceiver<Object>) receiver).receiveData(payload);
        }
        Scene scene = new Scene(root, 1100, 900);
        scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        root.applyCss();
        root.layout();
        return new Form(path, loader.getNamespace());
    }

    private static TextInputControl text(Form form, String id) {
        return (TextInputControl) form.nodes.get(id);
    }

    private static Button button(Form form, String id) {
        return (Button) form.nodes.get(id);
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<Object> combo(Form form, String id) {
        return (ComboBox<Object>) form.nodes.get(id);
    }

    private static void invoke(Button button) {
        button.getOnAction().handle(new ActionEvent(button, button));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private record Form(String path, Map<String, Object> nodes) {
    }
}
