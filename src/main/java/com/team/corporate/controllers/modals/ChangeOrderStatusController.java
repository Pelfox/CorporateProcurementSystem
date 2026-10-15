package com.team.corporate.controllers.modals;

import com.team.corporate.controllers.requests.OrderStatusChangeRequest;
import com.team.corporate.controllers.utils.OrderStatusDisplay;
import com.team.corporate.entities.Order;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.services.OrdersService;
import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.ResultCarrier;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class ChangeOrderStatusController implements DataReceiver<OrderStatusChangeRequest>, ResultCarrier<OrderStatus> {
    @FXML private Label orderIdLabel;
    @FXML private Label currentStatusLabel;
    @FXML private ComboBox<OrderStatus> newStatusComboBox;
    @FXML private Button changeButton;
    @FXML private Button cancelButton;

    private final OrdersService ordersService;

    private OrderStatusChangeRequest request;
    private OrderStatus currentStatus;
    private OrderStatus changedStatus;


    public ChangeOrderStatusController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @Override
    public void receiveData(OrderStatusChangeRequest request) {
        this.request = request;
        Order order = ordersService.getOrder(request.orderId())
                .orElseThrow(() -> new IllegalStateException("Заказ не найден. Возможно, он был удалён."));
        currentStatus = order.getStatus();

        orderIdLabel.setText("Заказ № " + order.getId());
        currentStatusLabel.setText(OrderStatusDisplay.name(currentStatus));
        currentStatusLabel.getStyleClass().add(OrderStatusDisplay.badgeStyle(currentStatus));
    }

    @Override
    public OrderStatus getResult() {
        return changedStatus;
    }

    @FXML
    public void initialize() {
        setupInputs();

        changeButton.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                newStatusComboBox.valueProperty()));
    }

    @FXML
    private void onChangeStatusClick() {
        if (request == null) {
            throw new IllegalStateException("Режим окна не инициализирован (payload не передан).");
        }

        if (isFormInvalid()) {
            return;
        }

        OrderStatus selectedStatus = newStatusComboBox.getValue();

        try {
            ordersService.changeStatus(request.managerId(), request.orderId(), selectedStatus);
            changedStatus = selectedStatus;
            close();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось изменить статус." : e.getMessage());
        }
    }

    @FXML
    private void onCancelClick() {
        close();
    }

    private boolean isFormInvalid() {
        OrderStatus selected = newStatusComboBox.getValue();
        return selected == null || selected == currentStatus;
    }

    private void close() {
        Window window = changeButton.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void setupInputs() {
        newStatusComboBox.setItems(FXCollections.observableArrayList(OrderStatus.values()));
        newStatusComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(OrderStatus status) {
                return status == null ? "" : OrderStatusDisplay.name(status);
            }

            @Override
            public OrderStatus fromString(String string) {
                return null;
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Изменение статуса");
        alert.setHeaderText(null);
        alert.initOwner(changeButton.getScene().getWindow());
        alert.showAndWait();
    }
}
