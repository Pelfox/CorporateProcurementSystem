package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.DialogService;
import com.team.corporate.controllers.dialog.ViewType;
import com.team.corporate.controllers.requests.OrderEditorRequest;
import com.team.corporate.entities.Category;
import com.team.corporate.entities.Inventory;
import com.team.corporate.entities.Product;
import com.team.corporate.entities.Warehouse;
import com.team.corporate.services.InventoriesService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class MainWindowController implements DataReceiver<UUID> {
    private final InventoriesService inventoriesService;
    private final DialogService dialogService;
    private final ObservableList<Inventory> inventoryList = FXCollections.observableArrayList();
    @FXML
    private TextField searchField;
    @FXML
    private TableView<Inventory> inventoryTable;
    @FXML
    private TableColumn<Inventory, String> productNameColumn;
    @FXML
    private TableColumn<Inventory, String> productSkuColumn;
    @FXML
    private TableColumn<Inventory, String> productCategoryColumn;
    @FXML
    private TableColumn<Inventory, String> warehouseNameColumn;
    @FXML
    private TableColumn<Inventory, Integer> quantityColumn;
    @FXML
    private TableColumn<Inventory, BigDecimal> productPriceColumn;
    @FXML
    private Button createOrderButton;
    private UUID currentUserId;

    public MainWindowController(InventoriesService inventoriesService, DialogService dialogService) {
        this.inventoriesService = inventoriesService;
        this.dialogService = dialogService;
    }

    @Override
    public void receiveData(UUID currentUserId) {
        this.currentUserId = currentUserId;
    }

    @FXML
    public void initialize() {
        setupTable();
        setupSearchAndFilter();
        loadData();

        createOrderButton.setOnAction(event -> openCreateOrderModal());
    }

    @FXML
    private void openCreateOrderModal() {
        Optional<UUID> created = dialogService.showModal(
                ViewType.CREATE_ORDER,
                inventoryTable.getScene().getWindow(),
                OrderEditorRequest.forCreate(currentUserId)
        );
        loadData();
    }

    private void setupTable() {
        productNameColumn.setCellValueFactory(cell -> {
            Product product = cell.getValue().getProduct();
            return new SimpleStringProperty(product.getName());
        });
        productSkuColumn.setCellValueFactory(cell -> {
            Product product = cell.getValue().getProduct();
            return new SimpleStringProperty(product.getSku());
        });
        productCategoryColumn.setCellValueFactory(cell -> {
            Category category = cell.getValue().getProduct().getCategory();
            return new SimpleStringProperty(category != null ? category.getName() : "-");
        });
        warehouseNameColumn.setCellValueFactory(cell -> {
            Warehouse warehouse = cell.getValue().getWarehouse();
            return new SimpleStringProperty(warehouse.getName());
        });
        quantityColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getQuantity())
        );
        productPriceColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getProduct().getPrice())
        );
    }

    private void setupSearchAndFilter() {
        FilteredList<Inventory> filteredInventoryList = new FilteredList<>(inventoryList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredInventoryList.setPredicate(inventory -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseString = newValue.toLowerCase();
                if (inventory.getProduct().getName().toLowerCase().contains(lowerCaseString)) {
                    return true;
                } else return inventory.getProduct().getSku().toLowerCase().contains(lowerCaseString);
            });
        });

        SortedList<Inventory> sortedInventoryList = new SortedList<>(filteredInventoryList);
        sortedInventoryList.comparatorProperty().bind(inventoryTable.comparatorProperty());

        inventoryTable.setItems(sortedInventoryList);
    }

    private void loadData() {
        inventoryList.setAll(inventoriesService.getAll());
    }
}
