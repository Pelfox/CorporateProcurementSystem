package com.team.corporate.controllers.modals;

import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.ResultCarrier;
import com.team.corporate.controllers.requests.WarehouseEditorRequest;
import com.team.corporate.controllers.utils.TextLengthInput;
import com.team.corporate.entities.Warehouse;
import com.team.corporate.services.WarehousesService;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.UUID;

public class WarehouseEditorController implements DataReceiver<WarehouseEditorRequest>, ResultCarrier<UUID> {
    private final WarehousesService warehousesService;
    @FXML
    private TextField nameField;
    @FXML
    private TextField locationField;
    @FXML
    private Label nameLengthLabel;
    @FXML
    private Label nameError;
    @FXML
    private Label locationLengthLabel;
    @FXML
    private Label locationError;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;
    private WarehouseEditorRequest request;
    private UUID savedWarehouseId;

    public WarehouseEditorController(WarehousesService warehousesService) {
        this.warehousesService = warehousesService;
    }

    @Override
    public void receiveData(WarehouseEditorRequest request) {
        this.request = request;
        if (request.isEditing()) {
            prefillForEdit(request.warehouseId());
        }
    }

    @Override
    public UUID getResult() {
        return savedWarehouseId;
    }

    @FXML
    public void initialize() {
        setupInputs();

        saveBtn.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                nameField.textProperty(), locationField.textProperty()));
    }

    @FXML
    private void onSaveClick() {
        if (request == null) {
            return;
        }

        if (isFormInvalid()) {
            return;
        }

        String name = TextLengthInput.text(nameField).strip();
        String address = TextLengthInput.text(locationField).isBlank() ? null : TextLengthInput.text(locationField).strip();

        try {
            Warehouse saved = request.isEditing()
                    ? warehousesService.saveWarehouse(request.warehouseId(), name, address)
                    : warehousesService.createWarehouse(name, address);
            savedWarehouseId = saved.getId();
            close();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось сохранить склад." : e.getMessage());
        }
    }

    @FXML
    private void onCancelClick() {
        close();
    }

    private void prefillForEdit(UUID warehouseId) {
        Warehouse warehouse = warehousesService.getWarehouse(warehouseId)
                .orElseThrow(() -> new IllegalStateException("Редактируемый склад не найден. Возможно, он был удален."));
        nameField.setText(warehouse.getName());
        locationField.setText(warehouse.getLocation() == null ? "" : warehouse.getLocation());
    }

    private boolean isFormInvalid() {
        return !TextLengthInput.isValid(nameField, true) || !TextLengthInput.isValid(locationField, false);
    }

    private void close() {
        Window window = saveBtn.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void setupInputs() {
        TextLengthInput.configure(nameField, nameLengthLabel, nameError, "Название склада");
        TextLengthInput.configure(locationField, locationLengthLabel, locationError, "Адрес");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Склад");
        alert.setHeaderText(null);
        alert.initOwner(saveBtn.getScene().getWindow());
        alert.showAndWait();
    }
}
