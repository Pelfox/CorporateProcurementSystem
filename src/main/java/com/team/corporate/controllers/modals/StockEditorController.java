package com.team.corporate.controllers.modals;

import com.team.corporate.controllers.dialog.ResultCarrier;
import com.team.corporate.controllers.utils.IntegerInput;
import com.team.corporate.entities.Inventory;
import com.team.corporate.entities.Product;
import com.team.corporate.entities.Warehouse;
import com.team.corporate.services.InventoriesService;
import com.team.corporate.services.ProductsService;
import com.team.corporate.services.WarehousesService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.UUID;

public class StockEditorController implements ResultCarrier<UUID> {
    private final InventoriesService inventoriesService;
    private final WarehousesService warehousesService;
    private final ProductsService productsService;
    @FXML
    private ComboBox<Warehouse> warehouseCombo;
    @FXML
    private ComboBox<Product> productCombo;
    @FXML
    private TextField quantityField;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;
    private UUID savedStockId;

    public StockEditorController(InventoriesService inventoriesService, WarehousesService warehousesService, ProductsService productsService) {
        this.inventoriesService = inventoriesService;
        this.warehousesService = warehousesService;
        this.productsService = productsService;
    }

    @Override
    public UUID getResult() {
        return savedStockId;
    }

    @FXML
    public void initialize() {
        setupInputs();
        IntegerInput.configure(quantityField, 0);

        saveBtn.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                warehouseCombo.valueProperty(),
                productCombo.valueProperty(),
                quantityField.textProperty()));
    }

    @FXML
    private void onSaveClick() {
        if (isFormInvalid()) {
            return;
        }

        Warehouse warehouse = warehouseCombo.getValue();
        Product product = productCombo.getValue();
        int quantity = IntegerInput.value(quantityField);

        try {
            Inventory saved = inventoriesService
                    .getAllByProductAndWarehouse(product.getId(), warehouse.getId())
                    .map(existing -> inventoriesService.updateInventory(existing.getId(), quantity))
                    .orElseGet(() -> inventoriesService.placeProductInWarehouse(
                            product.getId(), warehouse.getId(), quantity));
            savedStockId = saved.getId();
            close();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось сохранить остаток на складе." : e.getMessage());
        }
    }

    @FXML
    private void onCancelClick() {
        close();
    }

    private boolean isFormInvalid() {
        return warehouseCombo.getValue() == null
                || productCombo.getValue() == null
                || !warehouseCombo.getItems().contains(warehouseCombo.getValue())
                || !productCombo.getItems().contains(productCombo.getValue())
                || IntegerInput.value(quantityField) == null
                || IntegerInput.value(quantityField) < 0;
    }

    private void close() {
        Window window = saveBtn.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void setupInputs() {
        ObservableList<Warehouse> warehouses =
                FXCollections.observableArrayList(warehousesService.getAll());
        warehouses.sort(Comparator.comparing(Warehouse::getName));
        warehouseCombo.setItems(FXCollections.observableArrayList(warehouses));
        warehouseCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Warehouse warehouse) {
                return warehouse != null ? warehouse.getName() : "";
            }

            @Override
            public Warehouse fromString(String string) {
                return null;
            }
        });

        ObservableList<Product> products =
                FXCollections.observableArrayList(productsService.getAll());
        products.sort(Comparator.comparing(Product::getName));
        productCombo.setItems(FXCollections.observableArrayList(products));
        productCombo.setConverter(new StringConverter<>() {
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

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Остаток товара");
        alert.setHeaderText(null);
        alert.initOwner(saveBtn.getScene().getWindow());
        alert.showAndWait();
    }
}
