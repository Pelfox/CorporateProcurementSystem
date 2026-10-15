package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.dialog.DialogService;
import com.team.corporate.controllers.dialog.ViewType;
import com.team.corporate.controllers.requests.ProductEditorRequest;
import com.team.corporate.controllers.utils.MoneyFormat;
import com.team.corporate.entities.Category;
import com.team.corporate.entities.Product;
import com.team.corporate.services.CategoriesService;
import com.team.corporate.services.ProductsService;
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
import java.util.*;

public class CatalogController {

    @FXML private TextField searchField;
    @FXML private ComboBox<CategoryFilter> categoryComboBox;
    @FXML private ComboBox<SortMode> sortComboBox;

    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> numberColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> skuColumn;
    @FXML private TableColumn<Product, BigDecimal> priceColumn;
    @FXML private TableColumn<Product, String> categoryColumn;

    private final ProductsService productsService;
    private final CategoriesService categoriesService;
    private final DialogService dialogService;

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final Map<UUID, String> categoryNames = new HashMap<>();

    public CatalogController(ProductsService productsService,
                             CategoriesService categoriesService,
                             DialogService dialogService) {
        this.productsService = productsService;
        this.categoriesService = categoriesService;
        this.dialogService = dialogService;
    }

    @FXML
    public void initialize() {
        configureTable();
        configureFilters();
        configureButtons();
        loadData();
    }

    private void loadData() {
        products.setAll(productsService.getAll());
        categoryNames.clear();
        for (Category category : categoriesService.getAll()) {
            categoryNames.put(category.getId(), category.getName());
        }
    }

    private String categoryNameOf(Product product) {
        Category category = product.getCategory();
        return category == null ? "" : categoryNames.getOrDefault(category.getId(), "");
    }

    private void configureTable() {
        numberColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(shortId(cell.getValue().getId())));

        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));

        skuColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getSku()));

        priceColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getPrice()));
        priceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : MoneyFormat.format(value) + " ₽");
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        categoryColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(categoryNameOf(cell.getValue())));
    }

    private void configureFilters() {
        categoryComboBox.setItems(FXCollections.observableArrayList(
                CategoryFilter.all(categoriesService.getAll())));
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(CategoryFilter filter) { return filter == null ? "" : filter.label(); }
            @Override public CategoryFilter fromString(String string) { return null; }
        });

        sortComboBox.setItems(FXCollections.observableArrayList(SortMode.values()));
        sortComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(SortMode mode) { return mode == null ? "" : mode.label(); }
            @Override public SortMode fromString(String string) { return null; }
        });

        FilteredList<Product> filtered = new FilteredList<>(products, p -> true);
        Runnable reapply = () -> filtered.setPredicate(this::matchesFilters);
        searchField.textProperty().addListener((obs, old, value) -> reapply.run());
        categoryComboBox.valueProperty().addListener((obs, old, value) -> reapply.run());

        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(productsTable.comparatorProperty());
        productsTable.setItems(sorted);

        sortComboBox.valueProperty().addListener((obs, old, mode) -> applySortMode(mode));
        sortComboBox.setValue(SortMode.NAME_ASC);
        categoryComboBox.setValue(CategoryFilter.ALL);
    }

    private void applySortMode(SortMode mode) {
        productsTable.getSortOrder().clear();
        if (mode == null) {
            return;
        }
        switch (mode) {
            case NAME_ASC -> {
                nameColumn.setSortType(TableColumn.SortType.ASCENDING);
                productsTable.getSortOrder().add(nameColumn);
            }
            case NAME_DESC -> {
                nameColumn.setSortType(TableColumn.SortType.DESCENDING);
                productsTable.getSortOrder().add(nameColumn);
            }
            case PRICE_ASC -> {
                priceColumn.setSortType(TableColumn.SortType.ASCENDING);
                productsTable.getSortOrder().add(priceColumn);
            }
            case PRICE_DESC -> {
                priceColumn.setSortType(TableColumn.SortType.DESCENDING);
                productsTable.getSortOrder().add(priceColumn);
            }
        }
    }

    private void configureButtons() {
        editButton.disableProperty().bind(
                productsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(
                productsTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private boolean matchesFilters(Product product) {
        String query = searchField.getText();
        if (query != null && !query.isBlank()) {
            String q = query.strip().toLowerCase(Locale.ROOT);
            boolean byNumber = product.getId().toString().toLowerCase(Locale.ROOT).contains(q);
            boolean byName = product.getName().toLowerCase(Locale.ROOT).contains(q);
            boolean bySku = product.getSku().toLowerCase(Locale.ROOT).contains(q);
            if (!byNumber && !byName && !bySku) {
                return false;
            }
        }

        CategoryFilter filter = categoryComboBox.getValue();
        if (filter != null && filter.category() != null) {
            Category productCategory = product.getCategory();
            if (productCategory == null || !productCategory.getId().equals(filter.category().getId())) {
                return false;
            }
        }
        return true;
    }

    @FXML
    private void onAddClick() {
        Optional<UUID> saved = dialogService.showModal(
                ViewType.CREATE_PRODUCT,
                addButton.getScene().getWindow(),
                ProductEditorRequest.forCreate());
        if (saved.isPresent()) {
            loadData();
        }
    }

    @FXML
    private void onEditClick() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Optional<UUID> saved = dialogService.showModal(
                ViewType.EDIT_PRODUCT,
                editButton.getScene().getWindow(),
                ProductEditorRequest.forEdit(selected));
        if (saved.isPresent()) {
            loadData();
        }
    }

    @FXML
    private void onDeleteClick() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // TODO: перед productsService.deleteProduct(...) открыть подтверждение удаления.
        try {
            productsService.deleteProduct(selected.getId());
            loadData();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось удалить товар." : e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Каталог товаров");
        alert.setHeaderText(null);
        alert.initOwner(productsTable.getScene().getWindow());
        alert.showAndWait();
    }

    private record CategoryFilter(String label, Category category) {
        static final CategoryFilter ALL = new CategoryFilter("Все категории", null);

        static List<CategoryFilter> all(List<Category> categories) {
            List<CategoryFilter> filters = new ArrayList<>();
            filters.add(ALL);
            for (Category category : categories) {
                filters.add(new CategoryFilter(category.getName(), category));
            }
            return filters;
        }
    }

    private enum SortMode {
        NAME_ASC("По названию (А→Я)"),
        NAME_DESC("По названию (Я→А)"),
        PRICE_ASC("По цене (сначала дешёвые)"),
        PRICE_DESC("По цене (сначала дорогие)");

        private final String label;
        SortMode(String label) { this.label = label; }
        String label() { return label; }
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
