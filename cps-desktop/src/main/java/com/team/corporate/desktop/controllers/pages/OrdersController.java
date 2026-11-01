package com.team.corporate.desktop.controllers.pages;

import com.team.corporate.core.entities.*;
import com.team.corporate.desktop.controllers.dialog.DataReceiver;
import com.team.corporate.desktop.controllers.dialog.DialogService;
import com.team.corporate.desktop.controllers.dialog.ViewType;
import com.team.corporate.desktop.controllers.requests.OrderEditorRequest;
import com.team.corporate.desktop.controllers.requests.OrderStatusChangeRequest;
import com.team.corporate.desktop.controllers.session.UserSession;
import com.team.corporate.desktop.controllers.utils.*;
import com.team.corporate.desktop.controllers.utils.*;
import com.team.corporate.core.services.OrderItemsService;
import com.team.corporate.core.services.OrdersService;
import com.team.corporate.core.services.UsersService;
import com.team.corporate.desktop.controllers.utils.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.*;

public class OrdersController extends PageController implements DataReceiver<UserSession> {
    private final OrdersService orders;
    private final OrderItemsService items;
    private final UsersService users;
    private final DialogService dialogs;
    private final ObservableList<Order> rows = FXCollections.observableArrayList();
    private final Map<UUID, List<OrderItem>> lines = new HashMap<>();
    private final Map<UUID, String> usernames = new HashMap<>();
    @FXML
    private TextField searchField, priceFromField, priceToField;
    @FXML
    private ComboBox<OrderStatus> statusFilter;
    @FXML
    private ComboBox<User> userFilter;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Button createBtn, editBtn, statusBtn, deleteBtn;
    @FXML
    private TableView<Order> ordersTable;
    @FXML
    private TableColumn<Order, String> numCol, userCol, statusCol, dateCol, notesCol, priceCol;
    @FXML
    private TableColumn<Order, Number> itemsCol;
    @FXML
    private TableColumn<Order, BigDecimal> sumCol;
    private UserSession session;

    public OrdersController(OrdersService orders, OrderItemsService items, UsersService users, DialogService dialogs) {
        this.orders = orders;
        this.items = items;
        this.users = users;
        this.dialogs = dialogs;
    }

    @FXML
    public void initialize() {
        TextLengthInput.configure(searchField);
        DigitsOnlyInput.configure(priceFromField, priceToField);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        numCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getId().toString().substring(0, 8)));
        userCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(usernames.getOrDefault(cell.getValue().getUser().getId(), "")));
        statusCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(OrderStatusDisplay.name(cell.getValue().getStatus())));
        dateCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(DateTimeFormat.formatDateTime(DateTimeFormat.toLocalDateTime(cell.getValue().getCreatedAt()))));
        notesCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getNotes()));
        itemsCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(orderLines(cell.getValue()).stream().mapToLong(OrderItem::getQuantity).sum()));
        sumCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(total(cell.getValue())));
        sumCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : MoneyFormat.format(value) + " ₽");
            }
        });
        priceCol.setCellValueFactory(cell -> {
            List<OrderItem> orderLines = orderLines(cell.getValue());
            return new ReadOnlyStringWrapper(orderLines.size() == 1
                    ? MoneyFormat.format(orderLines.getFirst().getPurchasePrice()) + " ₽" : "—");
        });
        statusFilter.getItems().add(null);
        statusFilter.getItems().addAll(OrderStatus.values());
        statusFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(OrderStatus value) {
                return value == null ? "Все статусы" : OrderStatusDisplay.name(value);
            }

            @Override
            public OrderStatus fromString(String text) {
                return null;
            }
        });
        userFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(User value) {
                return value == null ? "Все пользователи" : value.getUsername();
            }

            @Override
            public User fromString(String text) {
                return null;
            }
        });
        sortCombo.getItems().setAll("Сначала новые", "Сначала старые", "По сумме ↑", "По сумме ↓");
        sortCombo.getSelectionModel().selectFirst();
        FilteredList<Order> filtered = new FilteredList<>(rows);
        Runnable refresh = () -> filtered.setPredicate(this::matchesFilters);
        searchField.textProperty().addListener((obs, old, value) -> refresh.run());
        priceFromField.textProperty().addListener((obs, old, value) -> refresh.run());
        priceToField.textProperty().addListener((obs, old, value) -> refresh.run());
        statusFilter.valueProperty().addListener((obs, old, value) -> refresh.run());
        userFilter.valueProperty().addListener((obs, old, value) -> refresh.run());
        sortCombo.valueProperty().addListener((obs, old, value) -> sortRows());
        var sorted = new javafx.collections.transformation.SortedList<>(filtered);
        sorted.comparatorProperty().bind(ordersTable.comparatorProperty());
        ordersTable.setItems(sorted);
        editBtn.disableProperty().bind(ordersTable.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.disableProperty().bind(ordersTable.getSelectionModel().selectedItemProperty().isNull());
        statusBtn.disableProperty().bind(Bindings.createBooleanBinding(
                () -> session == null || session.role() != UserRole.MANAGER
                        || ordersTable.getSelectionModel().getSelectedItem() == null
                        || ordersTable.getSelectionModel().getSelectedItem().getStatus() == OrderStatus.DELIVERED,
                ordersTable.getSelectionModel().selectedItemProperty()));
    }

    @Override
    public void receiveData(UserSession session) {
        this.session = session;
        loadData();
    }

    private void loadData() {
        lines.clear();
        for (OrderItem item : items.getAll())
            lines.computeIfAbsent(item.getOrder().getId(), ignored -> new ArrayList<>()).add(item);
        usernames.clear();
        List<User> allUsers = users.getAll();
        allUsers.forEach(user -> usernames.put(user.getId(), user.getUsername()));
        User selected = userFilter.getValue();
        userFilter.getItems().setAll(allUsers);
        userFilter.getItems().addFirst(null);
        userFilter.setValue(selected);
        rows.setAll(orders.getAll());
        sortRows();
    }

    private void sortRows() {
        ordersTable.getSortOrder().clear();
        Comparator<Order> comparator = switch (sortCombo.getSelectionModel().getSelectedIndex()) {
            case 1 -> Comparator.comparing(Order::getCreatedAt);
            case 2 -> Comparator.comparing(this::total);
            case 3 -> Comparator.comparing(this::total).reversed();
            default -> Comparator.comparing(Order::getCreatedAt).reversed();
        };
        rows.sort(comparator);
    }

    private boolean matchesFilters(Order order) {
        String query = TextLengthInput.text(searchField).strip().toLowerCase(Locale.ROOT);
        if (!query.isEmpty() && !order.getId().toString().contains(query)
                && !Objects.toString(order.getNotes(), "").toLowerCase(Locale.ROOT).contains(query)) return false;
        if (statusFilter.getValue() != null && order.getStatus() != statusFilter.getValue()) return false;
        if (userFilter.getValue() != null && !order.getUser().getId().equals(userFilter.getValue().getId()))
            return false;
        try {
            BigDecimal total = total(order);
            return (TextLengthInput.text(priceFromField).isBlank() || total.compareTo(DigitsOnlyInput.value(priceFromField)) >= 0)
                    && (TextLengthInput.text(priceToField).isBlank() || total.compareTo(DigitsOnlyInput.value(priceToField)) <= 0);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<OrderItem> orderLines(Order order) {
        return lines.getOrDefault(order.getId(), List.of());
    }

    private BigDecimal total(Order order) {
        return orderLines(order).stream().map(item -> item.getPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @FXML
    private void onCreateClick() {
        try {
            if (session == null) return;
            if (dialogs.showModal(ViewType.CREATE_ORDER, createBtn.getScene().getWindow(), OrderEditorRequest.forCreate(session.userId())).isPresent())
                loadData();

        } catch (RuntimeException e) {
            UiErrors.show(ordersTable.getScene() == null ? null : ordersTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    @FXML
    private void onEditClick() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null || session == null) return;
        TextInputDialog editor = new TextInputDialog(Objects.toString(selected.getNotes(), ""));
        editor.initOwner(editBtn.getScene().getWindow());
        editor.setTitle("Примечание к заказу");
        editor.setHeaderText(null);
        editor.setContentText("Примечание:");
        TextLengthInput.configure(editor.getEditor());
        editor.showAndWait().ifPresent(notes -> {
            try {
                if (!TextLengthInput.isValid(editor.getEditor(), false)) return;
                orders.updateNotes(session.userId(), selected.getId(), notes.isBlank() ? null : notes.strip());
                loadData();
            } catch (RuntimeException e) {
                showError(e);
            }
        });
    }

    @FXML
    private void onChangeStatusClick() {
        try {
            Order selected = ordersTable.getSelectionModel().getSelectedItem();
            if (session == null || session.role() != UserRole.MANAGER
                    || selected == null || selected.getStatus() == OrderStatus.DELIVERED) return;
            if (selected != null && dialogs.showModal(ViewType.CHANGE_ORDER_STATUS, statusBtn.getScene().getWindow(),
                    new OrderStatusChangeRequest(selected.getId(), session.userId())).isPresent()) loadData();

        } catch (RuntimeException e) {
            UiErrors.show(ordersTable.getScene() == null ? null : ordersTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    @FXML
    private void onDeleteClick() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null || !dialogs.confirmDelete(deleteBtn.getScene().getWindow(), "Заказ " + selected.getId()))
            return;
        try {
            orders.deleteOrder(selected.getId());
            loadData();
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void showError(RuntimeException error) {
        Alert alert = new Alert(Alert.AlertType.WARNING, Objects.toString(error.getMessage(), "Не удалось изменить заказ."), ButtonType.OK);
        alert.initOwner(ordersTable.getScene().getWindow());
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
