package com.team.corporate.desktop.controllers.pages;

import com.team.corporate.desktop.controllers.dialog.DialogService;
import com.team.corporate.desktop.controllers.dialog.ViewType;
import com.team.corporate.desktop.controllers.requests.WarehouseEditorRequest;
import com.team.corporate.desktop.controllers.utils.TextLengthInput;
import com.team.corporate.desktop.controllers.utils.UiErrors;
import com.team.corporate.core.entities.Inventory;
import com.team.corporate.core.entities.Product;
import com.team.corporate.core.entities.Warehouse;
import com.team.corporate.core.services.InventoriesService;
import com.team.corporate.core.services.ProductsService;
import com.team.corporate.core.services.WarehousesService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.*;

public class WarehousesController extends PageController {

    private final WarehousesService warehousesService;
    private final InventoriesService inventoriesService;
    private final ProductsService productsService;
    private final DialogService dialogService;
    private final ObservableList<Warehouse> warehouses = FXCollections.observableArrayList();
    private final ObservableList<Inventory> stockItems = FXCollections.observableArrayList();
    private final Map<UUID, Product> productsById = new HashMap<>();
    private final ObjectProperty<Warehouse> selectedWarehouse = new SimpleObjectProperty<>(null);
    private final ToggleGroup cardGroup = new ToggleGroup();
    @FXML
    private TextField searchField;
    @FXML
    private Button createWarehouseBtn;
    @FXML
    private Button editWarehouseBtn;
    @FXML
    private Button changeQtyBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private VBox warehousesListItems;
    @FXML
    private Label listPlaceholderLabel;
    @FXML
    private TableView<Inventory> inventoryTable;
    @FXML
    private TableColumn<Inventory, String> productCol;
    @FXML
    private TableColumn<Inventory, String> skuCol;
    @FXML
    private TableColumn<Inventory, Number> qtyCol;
    @FXML
    private Label stockPlaceholderLabel;

    public WarehousesController(WarehousesService warehousesService,
                                InventoriesService inventoriesService,
                                ProductsService productsService,
                                DialogService dialogService) {
        this.warehousesService = warehousesService;
        this.inventoriesService = inventoriesService;
        this.productsService = productsService;
        this.dialogService = dialogService;
    }

    private static String addressOf(Warehouse warehouse) {
        String location = warehouse.getLocation();
        return location == null || location.isBlank() ? "Адрес не указан" : location;
    }

    @FXML
    public void initialize() {
        TextLengthInput.configure(searchField);
        configureTable();
        inventoryTable.setItems(stockItems);

        cardGroup.selectedToggleProperty().addListener((obs, old, toggle) -> {
            selectedWarehouse.set(toggle == null ? null : (Warehouse) toggle.getUserData());
            reloadStock();
        });

        editWarehouseBtn.disableProperty().bind(selectedWarehouse.isNull());
        deleteBtn.disableProperty().bind(selectedWarehouse.isNull());

        listPlaceholderLabel.managedProperty().bind(listPlaceholderLabel.visibleProperty());
        listPlaceholderLabel.visibleProperty().bind(Bindings.isEmpty(warehousesListItems.getChildren()));

        stockPlaceholderLabel.textProperty().bind(Bindings.createStringBinding(
                () -> selectedWarehouse.get() == null ? "Выберите склад" : "На этом складе нет остатков",
                selectedWarehouse));

        searchField.textProperty().addListener((obs, old, value) -> rebuildCards());

        loadData();
    }

    private void loadData() {
        warehouses.setAll(warehousesService.getAll());

        productsById.clear();
        for (Product product : productsService.getAll()) {
            productsById.put(product.getId(), product);
        }

        selectedWarehouse.set(null);
        rebuildCards();
        reloadStock();
    }

    private void reloadStock() {
        try {
            Warehouse selected = selectedWarehouse.get();
            if (selected == null) {
                stockItems.clear();
                return;
            }
            stockItems.setAll(inventoriesService.getAllByWarehouse(selected.getId()));

        } catch (RuntimeException e) {
            UiErrors.show(inventoryTable.getScene() == null ? null : inventoryTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    private void rebuildCards() {
        UUID selectedId = selectedWarehouse.get() == null ? null : selectedWarehouse.get().getId();

        cardGroup.getToggles().clear();
        warehousesListItems.getChildren().clear();

        String query = searchField.getText();
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);

        for (Warehouse warehouse : warehouses) {
            if (!q.isEmpty() && !matchesSearch(warehouse, q)) {
                continue;
            }
            warehousesListItems.getChildren().add(createCard(warehouse));
        }

        if (selectedId != null) {
            for (Node node : warehousesListItems.getChildren()) {
                ToggleButton card = (ToggleButton) node;
                if (((Warehouse) card.getUserData()).getId().equals(selectedId)) {
                    card.setSelected(true);
                    break;
                }
            }
        }
    }

    private boolean matchesSearch(Warehouse warehouse, String q) {
        boolean byName = warehouse.getName().toLowerCase(Locale.ROOT).contains(q);
        String location = warehouse.getLocation();
        boolean byAddress = location != null && location.toLowerCase(Locale.ROOT).contains(q);
        return byName || byAddress;
    }

    private ToggleButton createCard(Warehouse warehouse) {
        Label nameLabel = new Label(warehouse.getName());
        nameLabel.getStyleClass().add("warehouse-item-name");
        nameLabel.setWrapText(true);

        Label addressLabel = new Label(addressOf(warehouse));
        addressLabel.getStyleClass().add("warehouse-item-address");
        addressLabel.setWrapText(true);

        VBox content = new VBox(4, nameLabel, addressLabel);

        ToggleButton card = new ToggleButton();
        card.getStyleClass().add("warehouse-item");
        card.setToggleGroup(cardGroup);
        card.setUserData(warehouse);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.setGraphic(content);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefHeight(84);

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                card.setSelected(true);
                openEditDialog(warehouse);
            }
        });
        return card;
    }

    private void configureTable() {
        productCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(productNameOf(cell.getValue())));
        skuCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(productSkuOf(cell.getValue())));
        qtyCol.setCellValueFactory(cell ->
                new ReadOnlyIntegerWrapper(cell.getValue().getQuantity()));
    }

    private String productNameOf(Inventory inventory) {
        Product product = productsById.get(inventory.getProduct().getId());
        return product == null ? "" : product.getName();
    }

    private String productSkuOf(Inventory inventory) {
        Product product = productsById.get(inventory.getProduct().getId());
        return product == null ? "" : product.getSku();
    }

    @FXML
    private void onCreateClick() {
        try {
            Optional<UUID> saved = dialogService.showModal(
                    ViewType.CREATE_WAREHOUSE,
                    createWarehouseBtn.getScene().getWindow(),
                    WarehouseEditorRequest.forCreate());
            if (saved.isPresent()) {
                loadData();
            }

        } catch (RuntimeException e) {
            UiErrors.show(inventoryTable.getScene() == null ? null : inventoryTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    @FXML
    private void onEditClick() {
        try {
            Warehouse selected = selectedWarehouse.get();
            if (selected == null) {
                return;
            }
            openEditDialog(selected);

        } catch (RuntimeException e) {
            UiErrors.show(inventoryTable.getScene() == null ? null : inventoryTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    private void openEditDialog(Warehouse warehouse) {
        Optional<UUID> saved = dialogService.showModal(
                ViewType.EDIT_WAREHOUSE,
                editWarehouseBtn.getScene().getWindow(),
                WarehouseEditorRequest.forEdit(warehouse));
        if (saved.isPresent()) {
            loadData();
        }
    }

    @FXML
    private void onChangeStockClick() {
        try {
            Optional<UUID> saved = dialogService.showModal(
                    ViewType.SET_STOCK,
                    changeQtyBtn.getScene().getWindow());
            if (saved.isPresent()) {
                reloadStock();
            }

        } catch (RuntimeException e) {
            UiErrors.show(inventoryTable.getScene() == null ? null : inventoryTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    @FXML
    private void onDeleteClick() {
        Warehouse selected = selectedWarehouse.get();
        if (selected == null) {
            return;
        }


        if (!dialogService.confirmDelete(inventoryTable.getScene().getWindow(), selected.getName())) return;

        try {
            warehousesService.deleteWarehouse(selected.getId());
            loadData();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось удалить склад." : e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Склады и остатки");
        alert.setHeaderText(null);
        alert.initOwner(inventoryTable.getScene().getWindow());
        alert.showAndWait();
    }
}
