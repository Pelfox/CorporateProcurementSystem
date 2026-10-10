package com.team.corporate.controllers;

import com.sun.javafx.scene.control.IntegerField;
import com.team.corporate.entities.Inventory;
import com.team.corporate.entities.Product;
import com.team.corporate.entities.User;
import com.team.corporate.services.OrdersService;
import com.team.corporate.services.ProductsService;
import com.team.corporate.services.ui.DataReceiver;
import com.team.corporate.services.ui.ResultCarrier;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateOrderController implements DataReceiver<UUID>, ResultCarrier<UUID> {
    @FXML private ComboBox<Product> productComboBox;
    @FXML private IntegerField quantityField;
    @FXML private Button addButton;
    @FXML private TableView<CartItem> orderItemsTable;
    @FXML private TableColumn<CartItem, String> productNameColumn;
    @FXML private TableColumn<CartItem, Integer> quantityColumn;
    @FXML private TableColumn<CartItem, BigDecimal> priceColumn;
    @FXML private TableColumn<CartItem, Void> removeColumn;
    @FXML private Label totalLabel;
    @FXML private TextArea notesArea;
    @FXML private Button createButton;
    @FXML private Button cancelButton;

    private final OrdersService ordersService;
    private final ProductsService productsService;

    private final ObservableList<CartItem> orderItems = FXCollections.observableArrayList();

    private static final int MAX_LINE_QUANTITY = 100_000;
    private static final DecimalFormat MONEY = buildMoneyFormat();

    private UUID currentUserId;
    private UUID createdOrderId;

    public CreateOrderController(OrdersService ordersService, ProductsService productsService) {
        this.ordersService = ordersService;
        this.productsService = productsService;
    }

    @FXML
    public void initialize() {
        setupInputs();
        configureTable();

        addButton.disableProperty().bind(productComboBox.valueProperty().isNull());
        createButton.disableProperty().bind(Bindings.isEmpty(orderItems));
        totalLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Итого: " + formatMoney(orderTotal()) + " ₽", orderItems));
    }

    @Override
    public void receiveData(UUID currentUserId) {
        this.currentUserId = currentUserId;
    }

    @Override
    public UUID getResult() {
        return createdOrderId;
    }

    private void setupInputs() {
        quantityField.setValue(1);
        List<Product> products = productsService.getAll();
        productComboBox.setItems(FXCollections.observableArrayList(products));
        productComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() : "";
            }

            @Override
            public Product fromString(String string) {
                return null;
            }
        });
    }

    private void configureTable() {
        orderItemsTable.setItems(orderItems);

        productNameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getProduct().getName()));

        quantityColumn.setCellValueFactory(cell ->
                new ReadOnlyIntegerWrapper(cell.getValue().getQuantity()).asObject());

        priceColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getTotalPrice()));
        priceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : formatMoney(value) + " ₽");
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        removeColumn.setSortable(false);
        removeColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("✕");
            {
                // TODO: Вынести в отдельный .css файл для разделения ответственности.
                deleteButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");
                deleteButton.setOnAction(event -> {
                    var row = getTableRow();
                    if (row != null && row.getItem() != null) {
                        orderItems.remove(row.getItem());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });
    }

    @FXML
    private void onAddClick() {
        Product product = productComboBox.getValue();
        if (product == null) {
            return;
        }

        int quantity = readQuantity();
        if (quantity < 1 || quantity > MAX_LINE_QUANTITY) {
            showAlert("Количество должно быть целым числом от 1 до " + MAX_LINE_QUANTITY + ".");
            return;
        }

        addLine(product, quantity);

        productComboBox.setValue(null);
        quantityField.setValue(1);
        productComboBox.requestFocus();
    }

    private int readQuantity() {
        Integer value = quantityField.getValue();
        return value == null ? 0 : value;
    }

    private void addLine(Product product, int quantity) {
        for (int i = 0; i < orderItems.size(); i++) {
            CartItem existing = orderItems.get(i);
            if (existing.getProduct().getId().equals(product.getId())) {
                int total = existing.getQuantity() + quantity;
                if (total > MAX_LINE_QUANTITY) {
                    showAlert("Максимальное количество позиций в заказе - " + MAX_LINE_QUANTITY);
                    return;
                }

                orderItems.set(i, new CartItem(existing.getProduct(), total));
                return;
            }
        }
        orderItems.add(new CartItem(product, quantity));
    }

    @FXML
    private void onCancelClick() {
        close();
    }

    @FXML
    private void onCreateClick() {
        if (orderItems.isEmpty()) {
            showAlert("В заказ необходимо добавить хотя бы один товар.");
            return;
        }

        List<OrdersService.Line> orderLines = new ArrayList<>(orderItems.size());
        for (CartItem line : orderItems) {
            orderLines.add(new OrdersService.Line(line.getProduct().getId(), line.getQuantity()));
        }

        try {
            createdOrderId = ordersService.createOrder(currentUserId, orderLines, readNotes());
            close();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось создать заказ." : e.getMessage());
        }
    }

    private String readNotes() {
        String text = notesArea.getText();
        return text == null || text.isBlank() ? null : text.strip();
    }

    private BigDecimal orderTotal() {
        return orderItems.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void close() {
        Window window = createButton.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, javafx.scene.control.ButtonType.OK);
        alert.setTitle("Новый заказ");
        alert.setHeaderText(null);
        alert.initOwner(createButton.getScene().getWindow());
        alert.showAndWait();
    }

    private static String formatMoney(BigDecimal amount) {
        return MONEY.format(amount);
    }

    private static DecimalFormat buildMoneyFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols);
    }

    public static class CartItem {
        private final Product product;
        private int quantity;

        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() { return product; }
        public int getQuantity() { return quantity; }

        public void setQuantity(int quantity) { this.quantity = quantity; }

        public BigDecimal getTotalPrice() {
            return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }
}
