package com.team.corporate.controllers.modals;

import com.sun.javafx.scene.control.IntegerField;
import com.team.corporate.controllers.requests.OrderEditorRequest;
import com.team.corporate.controllers.utils.MoneyFormat;
import com.team.corporate.entities.*;
import com.team.corporate.services.OrderItemsService;
import com.team.corporate.services.OrdersService;
import com.team.corporate.services.ProductsService;
import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.ResultCarrier;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
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

public class OrderEditorController implements DataReceiver<OrderEditorRequest>, ResultCarrier<UUID> {
    @FXML private Label warningLabel;
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
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final OrdersService ordersService;
    private final ProductsService productsService;
    private final OrderItemsService orderItemsService;

    private final ObservableList<CartItem> orderItems = FXCollections.observableArrayList();

    private static final int MAX_LINE_QUANTITY = 100_000;

    private final BooleanProperty locked = new SimpleBooleanProperty(false);

    private OrderEditorRequest request;
    private UUID savedOrderId;

    public OrderEditorController(OrdersService ordersService, ProductsService productsService, OrderItemsService orderItemsService) {
        this.ordersService = ordersService;
        this.productsService = productsService;
        this.orderItemsService = orderItemsService;
    }

    @FXML
    public void initialize() {
        setupInputs();
        configureTable();

        addButton.disableProperty().bind(productComboBox.valueProperty().isNull().or(locked));
        saveButton.disableProperty().bind(Bindings.isEmpty(orderItems).or(locked));
        warningLabel.visibleProperty().bind(locked);

        productComboBox.disableProperty().bind(locked);
        quantityField.editableProperty().bind(locked.not());
        notesArea.editableProperty().bind(locked.not());
        totalLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Итого: " + MoneyFormat.format(orderTotal()) + " ₽", orderItems));
    }

    @Override
    public void receiveData(OrderEditorRequest request) {
        this.request = request;
        saveButton.setText(request.isEditing() ? "Сохранить" : "Создать заказ");
        if (!request.isEditing()) {
            return;
        }
        Order order = ordersService.getOrder(request.orderId())
                .orElseThrow(() -> new IllegalStateException("Редактируемый заказ не найден. Возможно, он был удалён."));
        if (order.getStatus() != OrderStatus.CREATED) {
            locked.set(true);
        }
        prefillForEdit(order);
    }

    @Override
    public UUID getResult() {
        return savedOrderId;
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
                setText(empty || value == null ? null : MoneyFormat.format(value) + " ₽");
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        removeColumn.setSortable(false);
        removeColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("✕");
            {
                deleteButton.getStyleClass().add("btn-remove-icon");
                deleteButton.disableProperty().bind(locked);
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
    private void onSaveClick() {
        if (request == null) {
            throw new IllegalStateException("Режим окна не инициализирован (payload не передан).");
        }

        if (orderItems.isEmpty()) {
            showAlert("В заказ необходимо добавить хотя бы один товар.");
            return;
        }

        List<OrdersService.Line> orderLines = new ArrayList<>(orderItems.size());
        for (CartItem line : orderItems) {
            orderLines.add(new OrdersService.Line(line.getProduct().getId(), line.getQuantity()));
        }

        try {
            if (request.isEditing()) {
                ordersService.saveOrder(request.userId(), request.orderId(), orderLines, readNotes());
                savedOrderId = request.orderId();
            } else {
                savedOrderId = ordersService.createOrder(request.userId(), orderLines, readNotes());
            }
            close();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось сохранить заказ." : e.getMessage());
        }
    }

    private void prefillForEdit(Order order) {
        if (order.getNotes() != null) {
            notesArea.setText(order.getNotes());
        }
        for (OrderItem item : orderItemsService.getAllByOrder(order.getId())) {
            orderItems.add(new CartItem(item.getProduct(), item.getQuantity()));
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
        Window window = saveButton.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, javafx.scene.control.ButtonType.OK);
        alert.setTitle("Заказ");
        alert.setHeaderText(null);
        alert.initOwner(saveButton.getScene().getWindow());
        alert.showAndWait();
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
