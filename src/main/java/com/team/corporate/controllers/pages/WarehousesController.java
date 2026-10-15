package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.dialog.DialogService;
import com.team.corporate.controllers.dialog.ViewType;
import com.team.corporate.controllers.requests.WarehouseEditorRequest;
import com.team.corporate.entities.Inventory;
import com.team.corporate.entities.Product;
import com.team.corporate.entities.Warehouse;
import com.team.corporate.services.InventoriesService;
import com.team.corporate.services.ProductsService;
import com.team.corporate.services.WarehousesService;
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

public class WarehousesController {

    @FXML private TextField searchField;
    @FXML private Button createButton;
    @FXML private Button editButton;
    @FXML private Button changeStockButton;
    @FXML private Button deleteButton;

    @FXML private VBox cardsBox;
    @FXML private Label listPlaceholderLabel;
    @FXML private TableView<Inventory> stockTable;
    @FXML private TableColumn<Inventory, String> productColumn;
    @FXML private TableColumn<Inventory, String> skuColumn;
    @FXML private TableColumn<Inventory, Number> quantityColumn;
    @FXML private Label stockPlaceholderLabel;

    private final WarehousesService warehousesService;
    private final InventoriesService inventoriesService;
    private final ProductsService productsService;
    private final DialogService dialogService;

    private final ObservableList<Warehouse> warehouses = FXCollections.observableArrayList();
    private final ObservableList<Inventory> stockItems = FXCollections.observableArrayList();

    private final Map<UUID, Product> productsById = new HashMap<>();

    private final ObjectProperty<Warehouse> selectedWarehouse = new SimpleObjectProperty<>(null);
    private final ToggleGroup cardGroup = new ToggleGroup();

    public WarehousesController(WarehousesService warehousesService,
                                InventoriesService inventoriesService,
                                ProductsService productsService,
                                DialogService dialogService) {
        this.warehousesService = warehousesService;
        this.inventoriesService = inventoriesService;
        this.productsService = productsService;
        this.dialogService = dialogService;
    }

    @FXML
    public void initialize() {
        configureTable();
        stockTable.setItems(stockItems);

        cardGroup.selectedToggleProperty().addListener((obs, old, toggle) -> {
            selectedWarehouse.set(toggle == null ? null : (Warehouse) toggle.getUserData());
            reloadStock();
        });

        editButton.disableProperty().bind(selectedWarehouse.isNull());
        deleteButton.disableProperty().bind(selectedWarehouse.isNull());

        listPlaceholderLabel.managedProperty().bind(listPlaceholderLabel.visibleProperty());
        listPlaceholderLabel.visibleProperty().bind(Bindings.isEmpty(cardsBox.getChildren()));

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
        Warehouse selected = selectedWarehouse.get();
        if (selected == null) {
            stockItems.clear();
            return;
        }
        stockItems.setAll(inventoriesService.getAllByWarehouse(selected.getId()));
    }

    private void rebuildCards() {
        UUID selectedId = selectedWarehouse.get() == null ? null : selectedWarehouse.get().getId();

        cardGroup.getToggles().clear();
        cardsBox.getChildren().clear();

        String query = searchField.getText();
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);

        for (Warehouse warehouse : warehouses) {
            if (!q.isEmpty() && !matchesSearch(warehouse, q)) {
                continue;
            }
            cardsBox.getChildren().add(createCard(warehouse));
        }

        if (selectedId != null) {
            for (Node node : cardsBox.getChildren()) {
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
        nameLabel.getStyleClass().add("warehouse-card-name");
        nameLabel.setWrapText(true);

        Label addressLabel = new Label(addressOf(warehouse));
        addressLabel.getStyleClass().add("warehouse-card-address");
        addressLabel.setWrapText(true);

        VBox content = new VBox(nameLabel, addressLabel);
        content.getStyleClass().add("warehouse-card-content");

        ToggleButton card = new ToggleButton();
        card.getStyleClass().add("warehouse-card");
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
        productColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(productNameOf(cell.getValue())));
        skuColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(productSkuOf(cell.getValue())));
        quantityColumn.setCellValueFactory(cell ->
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

    private static String addressOf(Warehouse warehouse) {
        String location = warehouse.getLocation();
        return location == null || location.isBlank() ? "Адрес не указан" : location;
    }

    @FXML
    private void onCreateClick() {
        Optional<UUID> saved = dialogService.showModal(
                ViewType.CREATE_WAREHOUSE,
                createButton.getScene().getWindow(),
                WarehouseEditorRequest.forCreate());
        if (saved.isPresent()) {
            loadData();
        }
    }

    @FXML
    private void onEditClick() {
        Warehouse selected = selectedWarehouse.get();
        if (selected == null) {
            return;
        }
        openEditDialog(selected);
    }

    private void openEditDialog(Warehouse warehouse) {
        Optional<UUID> saved = dialogService.showModal(
                ViewType.EDIT_WAREHOUSE,
                editButton.getScene().getWindow(),
                WarehouseEditorRequest.forEdit(warehouse));
        if (saved.isPresent()) {
            loadData();
        }
    }

    @FXML
    private void onChangeStockClick() {
        Optional<UUID> saved = dialogService.showModal(
                ViewType.SET_STOCK,
                changeStockButton.getScene().getWindow());
        if (saved.isPresent()) {
            reloadStock();
        }
    }

    @FXML
    private void onDeleteClick() {
        Warehouse selected = selectedWarehouse.get();
        if (selected == null) {
            return;
        }

        // TODO: перед warehousesService.deleteWarehouse(...) открыть модалку подтверждения.

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
        alert.initOwner(stockTable.getScene().getWindow());
        alert.showAndWait();
    }
}
