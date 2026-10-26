package com.team.corporate.desktop.controllers.modals;

import com.team.corporate.desktop.controllers.dialog.DataReceiver;
import com.team.corporate.desktop.controllers.dialog.ResultCarrier;
import com.team.corporate.desktop.controllers.requests.OrderEditorRequest;
import com.team.corporate.desktop.controllers.utils.IntegerInput;
import com.team.corporate.desktop.controllers.utils.MoneyFormat;
import com.team.corporate.desktop.controllers.utils.TextLengthInput;
import com.team.corporate.core.entities.Order;
import com.team.corporate.core.entities.OrderItem;
import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.entities.Product;
import com.team.corporate.core.services.OrderItemsService;
import com.team.corporate.core.services.OrdersService;
import com.team.corporate.core.services.ProductsService;
import com.team.corporate.core.services.StockService;
import com.team.corporate.core.utils.EntityValidation;
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
import java.util.Map;
import java.util.UUID;

public class OrderEditorController implements DataReceiver<OrderEditorRequest>, ResultCarrier<UUID> {
    private static final int MAX_LINE_QUANTITY = EntityValidation.ORDER_LINE_MAX_QUANTITY;
    private final OrdersService ordersService;
    private final ProductsService productsService;
    private final StockService stockService;
    private final OrderItemsService orderItemsService;
    private final ObservableList<CartItem> orderItems = FXCollections.observableArrayList();
    private final BooleanProperty locked = new SimpleBooleanProperty(false);
    @FXML
    private Label warningLabel;
    @FXML
    private ComboBox<Product> productCombo;
    @FXML
    private TextField quantityField;
    @FXML
    private Button addItemBtn;
    @FXML
    private Label addItemError;
    @FXML
    private TableView<CartItem> itemsTable;
    @FXML
    private TableColumn<CartItem, String> productCol;
    @FXML
    private TableColumn<CartItem, Integer> quantityCol;
    @FXML
    private TableColumn<CartItem, BigDecimal> sumCol;
    @FXML
    private TableColumn<CartItem, Void> removeCol;
    @FXML
    private Label totalLabel;
    @FXML
    private TextArea notesArea;
    @FXML
    private Button createBtn;
    @FXML
    private Button cancelBtn;
    private Map<UUID, Long> available = Map.of();
    private OrderEditorRequest request;
    private UUID savedOrderId;

    public OrderEditorController(OrdersService ordersService, ProductsService productsService, OrderItemsService orderItemsService, StockService stockService) {
        this.ordersService = ordersService;
        this.productsService = productsService;
        this.stockService = stockService;
        this.orderItemsService = orderItemsService;
    }

    private static DecimalFormat buildMoneyFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols);
    }

    @FXML
    public void initialize() {
        setupInputs();
        configureTable();

        var invalidQuantity = Bindings.createBooleanBinding(this::invalidQuantity,
                productCombo.valueProperty(), quantityField.textProperty(), orderItems);
        addItemBtn.disableProperty().bind(productCombo.valueProperty().isNull().or(locked).or(invalidQuantity));
        addItemError.visibleProperty().bind(productCombo.valueProperty().isNotNull().and(invalidQuantity));
        addItemError.managedProperty().bind(addItemError.visibleProperty());
        addItemError.textProperty().bind(Bindings.createStringBinding(() ->
                        "Введите количество от 1 до " + Math.min(lineCapacity(productCombo.getValue()), remaining(productCombo.getValue()))
                                + ". Доступно на складах: " + remaining(productCombo.getValue()) + ".",
                productCombo.valueProperty(), quantityField.textProperty(), orderItems));
        createBtn.disableProperty().bind(Bindings.createBooleanBinding(this::isFormInvalid,
                orderItems, locked, notesArea.textProperty()));
        warningLabel.visibleProperty().bind(locked);
        warningLabel.managedProperty().bind(locked);

        productCombo.disableProperty().bind(locked);
        quantityField.editableProperty().bind(locked.not());
        notesArea.editableProperty().bind(locked.not());
        totalLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Итого: " + MoneyFormat.format(orderTotal()) + " ₽", orderItems));
    }

    @Override
    public void receiveData(OrderEditorRequest request) {
        this.request = request;
        createBtn.setText(request.isEditing() ? "Сохранить" : "Создать заказ");
        if (!request.isEditing()) {
            refreshProducts();
            return;
        }
        Order order = ordersService.getOrder(request.orderId())
                .orElseThrow(() -> new IllegalStateException("Редактируемый заказ не найден. Возможно, он был удалён."));
        if (order.getStatus() != OrderStatus.CREATED) {
            locked.set(true);
        }
        prefillForEdit(order);
        refreshProducts();
    }

    @Override
    public UUID getResult() {
        return savedOrderId;
    }

    private void setupInputs() {
        IntegerInput.configure(quantityField, 1, MAX_LINE_QUANTITY);
        TextLengthInput.configure(notesArea);

        productCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product product) {
                return product != null ? product.getName() + " — доступно: " + remaining(product) : "";
            }

            @Override
            public Product fromString(String string) {
                return null;
            }
        });
    }

    private void refreshProducts() {
        available = stockService.getAvailableQuantities(request != null && request.isEditing() ? request.orderId() : null);
        List<Product> products = productsService.getAll().stream().filter(product -> remaining(product) > 0).toList();
        productCombo.setItems(FXCollections.observableArrayList(products));
        productCombo.setValue(null);
        productCombo.setPromptText(products.isEmpty() ? "Нет товаров в наличии" : "— Выберите товар —");
    }

    private long remaining(Product product) {
        if (product == null) return 0;
        long inCart = orderItems.stream().filter(item -> item.getProduct().getId().equals(product.getId()))
                .mapToLong(CartItem::getQuantity).sum();
        return Math.max(0, available.getOrDefault(product.getId(), 0L) - inCart);
    }

    private boolean invalidQuantity() {
        int quantity = readQuantity();
        return quantity < 1 || quantity > lineCapacity(productCombo.getValue())
                || quantity > remaining(productCombo.getValue());
    }

    private long lineCapacity(Product product) {
        if (product == null) return MAX_LINE_QUANTITY;
        long inCart = orderItems.stream().filter(item -> item.getProduct().getId().equals(product.getId()))
                .mapToLong(CartItem::getQuantity).sum();
        return Math.max(0, MAX_LINE_QUANTITY - inCart);
    }

    private void configureTable() {
        itemsTable.setItems(orderItems);

        productCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getProduct().getName()));

        quantityCol.setCellValueFactory(cell ->
                new ReadOnlyIntegerWrapper(cell.getValue().getQuantity()).asObject());

        sumCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getTotalPrice()));
        sumCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : MoneyFormat.format(value) + " ₽");
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        removeCol.setSortable(false);
        removeCol.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("✕");

            {
                deleteButton.getStyleClass().add("btn-remove-icon");
                deleteButton.disableProperty().bind(locked);
                deleteButton.setOnAction(event -> {
                    var row = getTableRow();
                    if (!locked.get() && row != null && row.getItem() != null) {
                        orderItems.remove(row.getItem());
                        refreshProductsSafely();
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
        Product product = productCombo.getValue();
        if (request == null || locked.get() || product == null
                || !productCombo.getItems().contains(product) || invalidQuantity()) {
            return;
        }
        try {
            available = stockService.getAvailableQuantities(request.isEditing() ? request.orderId() : null);
            int quantity = readQuantity();
            if (quantity > remaining(product)) {
                showAlert("Недостаточно товара «" + product.getName() + "». Доступно: " + remaining(product) + ".");
                refreshProducts();
                return;
            }
            if (quantity < 1 || quantity > MAX_LINE_QUANTITY) {
                showAlert("Количество должно быть целым числом от 1 до " + MAX_LINE_QUANTITY + ".");
                return;
            }

            addLine(product, quantity);

            productCombo.setValue(null);
            refreshProducts();
            quantityField.setText("1");
            productCombo.requestFocus();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось проверить наличие товара." : e.getMessage());
        }
    }

    private int readQuantity() {
        Integer value = IntegerInput.value(quantityField);
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
        if (request == null || isFormInvalid()) {
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
            refreshProductsSafely();
        }
    }

    private boolean isFormInvalid() {
        return locked.get() || orderItems.isEmpty() || !TextLengthInput.isValid(notesArea, false)
                || orderItems.stream().anyMatch(line -> line.getProduct() == null
                || line.getProduct().getId() == null || line.getQuantity() < 1 || line.getQuantity() > MAX_LINE_QUANTITY);
    }

    private void refreshProductsSafely() {
        try {
            refreshProducts();
        } catch (RuntimeException e) {
            productCombo.getItems().clear();
            productCombo.setValue(null);
            showAlert("Не удалось обновить список товаров. Откройте форму повторно.");
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
        Window window = createBtn.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, javafx.scene.control.ButtonType.OK);
        alert.setTitle("Заказ");
        alert.setHeaderText(null);
        alert.initOwner(createBtn.getScene().getWindow());
        alert.showAndWait();
    }

    public static class CartItem {
        private final Product product;
        private int quantity;

        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() {
            return product;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getTotalPrice() {
            return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }
}
