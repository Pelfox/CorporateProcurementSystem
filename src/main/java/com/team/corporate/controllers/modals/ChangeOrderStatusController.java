package com.team.corporate.controllers.modals;

import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.ResultCarrier;
import com.team.corporate.controllers.requests.OrderStatusChangeRequest;
import com.team.corporate.controllers.utils.OrderStatusDisplay;
import com.team.corporate.entities.Order;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.services.OrdersService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class ChangeOrderStatusController implements DataReceiver<OrderStatusChangeRequest>, ResultCarrier<OrderStatus> {
    private final OrdersService ordersService;
    @FXML
    private Label orderNumberLabel;
    @FXML
    private Label currentStatusLabel;
    @FXML
    private ComboBox<OrderStatus> newStatusCombo;
    @FXML
    private Button changeBtn;
    @FXML
    private Button cancelBtn;
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

        orderNumberLabel.setText("Заказ № " + order.getId());
        currentStatusLabel.setText(OrderStatusDisplay.name(currentStatus));
        currentStatusLabel.getStyleClass().add(OrderStatusDisplay.badgeStyle(currentStatus));
        if (currentStatus == OrderStatus.DELIVERED) newStatusCombo.getItems().clear();
        else newStatusCombo.getItems().remove(currentStatus);
    }

    @Override
    public OrderStatus getResult() {
        return changedStatus;
    }

    @FXML
    public void initialize() {
        setupInputs();

        changeBtn.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                newStatusCombo.valueProperty()));
    }

    @FXML
    private void onChangeStatusClick() {
        if (request == null) {
            return;
        }

        if (isFormInvalid()) {
            return;
        }

        OrderStatus selectedStatus = newStatusCombo.getValue();

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
        OrderStatus selected = newStatusCombo.getValue();
        return selected == null || selected == currentStatus || currentStatus == OrderStatus.DELIVERED
                || !newStatusCombo.getItems().contains(selected);
    }

    private void close() {
        Window window = changeBtn.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void setupInputs() {
        newStatusCombo.setItems(FXCollections.observableArrayList(OrderStatus.values()));
        newStatusCombo.setConverter(new StringConverter<>() {
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
        alert.initOwner(changeBtn.getScene().getWindow());
        alert.showAndWait();
    }
}
