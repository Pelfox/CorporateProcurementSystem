package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.session.UserSession;
import com.team.corporate.controllers.utils.DateTimeFormat;
import com.team.corporate.controllers.utils.MoneyFormat;
import com.team.corporate.controllers.utils.OrderStatusDisplay;
import com.team.corporate.controllers.utils.TextLengthInput;
import com.team.corporate.entities.Order;
import com.team.corporate.entities.OrderItem;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.services.OrderItemsService;
import com.team.corporate.services.OrdersService;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class MyOrdersController extends PageController implements DataReceiver<UserSession> {
    private final OrdersService ordersService;
    private final OrderItemsService orderItemsService;
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final Map<UUID, OrderSummary> summaries = new HashMap<>();
    @FXML
    private Label welcomeLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<StatusFilter> statusFilter;
    @FXML
    private DatePicker dateFromPicker;
    @FXML
    private DatePicker dateToPicker;
    @FXML
    private Button clearDatesBtn;
    @FXML
    private Label dateError;
    @FXML
    private ComboBox<SortMode> sortCombo;
    @FXML
    private TableView<Order> ordersTable;
    @FXML
    private TableColumn<Order, String> numCol;
    @FXML
    private TableColumn<Order, OrderStatus> statusCol;
    @FXML
    private TableColumn<Order, Number> itemsCol;
    @FXML
    private TableColumn<Order, BigDecimal> sumCol;
    @FXML
    private TableColumn<Order, LocalDateTime> dateCol;
    @FXML
    private TableColumn<Order, LocalDateTime> updatedColumn;
    @FXML
    private TableColumn<Order, String> notesColumn;
    private UserSession session;

    public MyOrdersController(OrdersService ordersService, OrderItemsService orderItemsService) {
        this.ordersService = ordersService;
        this.orderItemsService = orderItemsService;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    @Override
    public void receiveData(UserSession session) {
        this.session = session;
        welcomeLabel.setText("Здравствуйте, " + session.username() + "!");
        loadData();
    }

    @FXML
    public void initialize() {
        TextLengthInput.configure(searchField);
        configureTable();
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        configureFilters();
    }

    private void loadData() {
        orders.setAll(ordersService.getAllByUser(session.userId()));

        summaries.clear();
        for (OrderItem item : orderItemsService.getAll()) {
            OrderSummary current = summaries.getOrDefault(item.getOrder().getId(), OrderSummary.EMPTY);
            summaries.put(item.getOrder().getId(), current.plus(item));
        }
    }

    private OrderSummary summaryOf(Order order) {
        return order == null ? OrderSummary.EMPTY
                : summaries.getOrDefault(order.getId(), OrderSummary.EMPTY);
    }

    private void configureTable() {
        numCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(shortId(cell.getValue().getId())));

        statusCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getStatus()));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(OrderStatus status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeIf(style -> style.startsWith("status-"));
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(OrderStatusDisplay.name(status));
                    getStyleClass().add(OrderStatusDisplay.badgeStyle(status));
                }
            }
        });

        itemsCol.setCellValueFactory(cell ->
                new ReadOnlyIntegerWrapper(summaryOf(cell.getValue()).itemCount()));

        sumCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(summaryOf(cell.getValue()).total()));
        sumCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : MoneyFormat.format(value) + " ₽");
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        dateCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(DateTimeFormat.toLocalDateTime(cell.getValue().getCreatedAt())));
        dateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : DateTimeFormat.formatDateTime(value));
            }
        });
        updatedColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(DateTimeFormat.toLocalDateTime(cell.getValue().getUpdatedAt())));
        updatedColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : DateTimeFormat.formatDateTime(value));
            }
        });

        notesColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getNotes() == null ? "" : cell.getValue().getNotes()));
    }

    private void configureFilters() {
        dateError.visibleProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(() ->
                        dateFromPicker.getValue() != null && dateToPicker.getValue() != null
                                && dateFromPicker.getValue().isAfter(dateToPicker.getValue()),
                dateFromPicker.valueProperty(), dateToPicker.valueProperty()));
        dateError.managedProperty().bind(dateError.visibleProperty());
        clearDatesBtn.disableProperty().bind(dateFromPicker.valueProperty().isNull()
                .and(dateToPicker.valueProperty().isNull()));
        statusFilter.setItems(FXCollections.observableArrayList(StatusFilter.all()));
        statusFilter.setValue(StatusFilter.ALL);
        statusFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(StatusFilter filter) {
                return filter == null ? "" : filter.label();
            }

            @Override
            public StatusFilter fromString(String string) {
                return null;
            }
        });

        sortCombo.setItems(FXCollections.observableArrayList(SortMode.values()));
        sortCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(SortMode mode) {
                return mode == null ? "" : mode.label();
            }

            @Override
            public SortMode fromString(String string) {
                return null;
            }
        });

        FilteredList<Order> filtered = new FilteredList<>(orders, p -> true);
        Runnable reapply = () -> filtered.setPredicate(this::matchesFilters);
        searchField.textProperty().addListener((obs, old, value) -> reapply.run());
        statusFilter.valueProperty().addListener((obs, old, value) -> reapply.run());
        dateFromPicker.valueProperty().addListener((obs, old, value) -> reapply.run());
        dateToPicker.valueProperty().addListener((obs, old, value) -> reapply.run());

        SortedList<Order> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(ordersTable.comparatorProperty());
        ordersTable.setItems(sorted);

        sortCombo.valueProperty().addListener((obs, old, mode) -> applySortMode(mode));
        sortCombo.setValue(SortMode.NEWEST_FIRST);
    }

    @FXML
    private void onClearDatesClick() {
        dateFromPicker.setValue(null);
        dateToPicker.setValue(null);
    }

    private void applySortMode(SortMode mode) {
        ordersTable.getSortOrder().clear();
        if (mode == null) {
            return;
        }
        dateCol.setSortType(mode == SortMode.NEWEST_FIRST
                ? TableColumn.SortType.DESCENDING
                : TableColumn.SortType.ASCENDING);
        ordersTable.getSortOrder().add(dateCol);
    }

    private boolean matchesFilters(Order order) {
        String query = searchField.getText();
        if (query != null && !query.isBlank()) {
            String q = query.strip().toLowerCase(Locale.ROOT);
            boolean byNumber = order.getId().toString().toLowerCase(Locale.ROOT).contains(q);
            String notes = order.getNotes() == null ? "" : order.getNotes().toLowerCase(Locale.ROOT);
            if (!byNumber && !notes.contains(q)) {
                return false;
            }
        }

        StatusFilter status = statusFilter.getValue();
        if (status != null && status.status() != null && order.getStatus() != status.status()) {
            return false;
        }

        LocalDate createdDate = DateTimeFormat.toLocalDate(order.getCreatedAt());
        LocalDate from = dateFromPicker.getValue();
        if (from != null && createdDate.isBefore(from)) {
            return false;
        }
        LocalDate to = dateToPicker.getValue();
        return to == null || !createdDate.isAfter(to);
    }

    private enum SortMode {
        NEWEST_FIRST("Сначала новые"),
        OLDEST_FIRST("Сначала старые");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    private record OrderSummary(int itemCount, BigDecimal total) {
        static final OrderSummary EMPTY = new OrderSummary(0, BigDecimal.ZERO);

        OrderSummary plus(OrderItem item) {
            return new OrderSummary(itemCount + 1,
                    total.add(item.getPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
        }
    }

    private record StatusFilter(String label, OrderStatus status) {
        static final StatusFilter ALL = new StatusFilter("Все статусы", null);

        static List<StatusFilter> all() {
            List<StatusFilter> filters = new ArrayList<>();
            filters.add(ALL);
            for (OrderStatus status : OrderStatus.values()) {
                filters.add(new StatusFilter(OrderStatusDisplay.name(status), status));
            }
            return filters;
        }
    }
}
