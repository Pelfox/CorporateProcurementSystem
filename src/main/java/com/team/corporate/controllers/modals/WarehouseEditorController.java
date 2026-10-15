package com.team.corporate.controllers.modals;

import com.team.corporate.controllers.requests.WarehouseEditorRequest;
import com.team.corporate.entities.Warehouse;
import com.team.corporate.services.WarehousesService;
import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.ResultCarrier;
import com.team.corporate.utils.EntityValidation;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.UUID;

public class WarehouseEditorController implements DataReceiver<WarehouseEditorRequest>, ResultCarrier<UUID> {
    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final WarehousesService warehousesService;

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

        saveButton.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                nameField.textProperty()));
    }

    @FXML
    private void onSaveClick() {
        if (request == null) {
            throw new IllegalStateException("Режим окна не инициализирован (payload не передан).");
        }

        if (isFormInvalid()) {
            return;
        }

        String name = nameField.getText().strip();
        String address = addressField.getText().isBlank() ? null : addressField.getText().strip();

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
        addressField.setText(warehouse.getLocation());
    }

    private boolean isFormInvalid() {
        return nameField.getText().isBlank();
    }

    private void close() {
        Window window = saveButton.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void setupInputs() {
        nameField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= EntityValidation.TEXT_MAX_LENGTH ? change : null));
        addressField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= EntityValidation.TEXT_MAX_LENGTH ? change : null));
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Склад");
        alert.setHeaderText(null);
        alert.initOwner(saveButton.getScene().getWindow());
        alert.showAndWait();
    }
}
