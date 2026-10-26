package com.team.corporate.desktop.controllers.pages;

import com.team.corporate.desktop.controllers.dialog.DialogService;
import com.team.corporate.desktop.controllers.dialog.ViewType;
import com.team.corporate.desktop.controllers.requests.ProductEditorRequest;
import com.team.corporate.desktop.controllers.utils.DigitsOnlyInput;
import com.team.corporate.desktop.controllers.utils.MoneyFormat;
import com.team.corporate.desktop.controllers.utils.TextLengthInput;
import com.team.corporate.desktop.controllers.utils.UiErrors;
import com.team.corporate.core.entities.Category;
import com.team.corporate.core.entities.Product;
import com.team.corporate.core.services.CategoriesService;
import com.team.corporate.core.services.ProductsService;
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

public class CatalogController extends PageController {

    private final ProductsService productsService;
    private final CategoriesService categoriesService;
    private final DialogService dialogService;
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final Map<UUID, String> categoryNames = new HashMap<>();
    @FXML
    private TextField searchField;
    @FXML
    private TextField priceFromField, priceToField;
    @FXML
    private ComboBox<CategoryFilter> categoryFilter;
    @FXML
    private ComboBox<SortMode> sortCombo;
    @FXML
    private Button addBtn;
    @FXML
    private Button editBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> idCol;
    @FXML
    private TableColumn<Product, String> nameCol;
    @FXML
    private TableColumn<Product, String> skuCol;
    @FXML
    private TableColumn<Product, BigDecimal> priceCol;
    @FXML
    private TableColumn<Product, String> categoryCol;

    public CatalogController(ProductsService productsService,
                             CategoriesService categoriesService,
                             DialogService dialogService) {
        this.productsService = productsService;
        this.categoriesService = categoriesService;
        this.dialogService = dialogService;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    @FXML
    public void initialize() {
        TextLengthInput.configure(searchField);
        DigitsOnlyInput.configure(priceFromField, priceToField);
        configureTable();
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
        idCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(shortId(cell.getValue().getId())));

        nameCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));

        skuCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getSku()));

        priceCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getPrice()));
        priceCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : MoneyFormat.format(value) + " ₽");
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        categoryCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(categoryNameOf(cell.getValue())));
    }

    private void configureFilters() {
        categoryFilter.setItems(FXCollections.observableArrayList(
                CategoryFilter.all(categoriesService.getAll())));
        categoryFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(CategoryFilter filter) {
                return filter == null ? "" : filter.label();
            }

            @Override
            public CategoryFilter fromString(String string) {
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

        FilteredList<Product> filtered = new FilteredList<>(products, p -> true);
        Runnable reapply = () -> filtered.setPredicate(this::matchesFilters);
        searchField.textProperty().addListener((obs, old, value) -> reapply.run());
        priceFromField.textProperty().addListener((obs, old, value) -> reapply.run());
        priceToField.textProperty().addListener((obs, old, value) -> reapply.run());
        categoryFilter.valueProperty().addListener((obs, old, value) -> reapply.run());

        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(productsTable.comparatorProperty());
        productsTable.setItems(sorted);

        sortCombo.valueProperty().addListener((obs, old, mode) -> applySortMode(mode));
        sortCombo.setValue(SortMode.NAME_ASC);
        categoryFilter.setValue(CategoryFilter.ALL);
    }

    private void applySortMode(SortMode mode) {
        productsTable.getSortOrder().clear();
        if (mode == null) {
            return;
        }
        switch (mode) {
            case NAME_ASC -> {
                nameCol.setSortType(TableColumn.SortType.ASCENDING);
                productsTable.getSortOrder().add(nameCol);
            }
            case NAME_DESC -> {
                nameCol.setSortType(TableColumn.SortType.DESCENDING);
                productsTable.getSortOrder().add(nameCol);
            }
            case PRICE_ASC -> {
                priceCol.setSortType(TableColumn.SortType.ASCENDING);
                productsTable.getSortOrder().add(priceCol);
            }
            case PRICE_DESC -> {
                priceCol.setSortType(TableColumn.SortType.DESCENDING);
                productsTable.getSortOrder().add(priceCol);
            }
        }
    }

    private void configureButtons() {
        editBtn.disableProperty().bind(
                productsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.disableProperty().bind(
                productsTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private boolean matchesFilters(Product product) {
        try {
            if (!TextLengthInput.text(priceFromField).isBlank() && product.getPrice().compareTo(
                    DigitsOnlyInput.value(priceFromField)) < 0) return false;
            if (!TextLengthInput.text(priceToField).isBlank() && product.getPrice().compareTo(
                    DigitsOnlyInput.value(priceToField)) > 0) return false;
        } catch (NumberFormatException e) {
            return false;
        }
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

        CategoryFilter filter = categoryFilter.getValue();
        if (filter != null && filter.category() != null) {
            Category productCategory = product.getCategory();
            return productCategory != null && productCategory.getId().equals(filter.category().getId());
        }
        return true;
    }

    @FXML
    private void onAddClick() {
        try {
            Optional<UUID> saved = dialogService.showModal(
                    ViewType.CREATE_PRODUCT,
                    addBtn.getScene().getWindow(),
                    ProductEditorRequest.forCreate());
            if (saved.isPresent()) {
                loadData();
            }

        } catch (RuntimeException e) {
            UiErrors.show(productsTable.getScene() == null ? null : productsTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    @FXML
    private void onEditClick() {
        try {
            Product selected = productsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Optional<UUID> saved = dialogService.showModal(
                    ViewType.EDIT_PRODUCT,
                    editBtn.getScene().getWindow(),
                    ProductEditorRequest.forEdit(selected));
            if (saved.isPresent()) {
                loadData();
            }

        } catch (RuntimeException e) {
            UiErrors.show(productsTable.getScene() == null ? null : productsTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    @FXML
    private void onDeleteClick() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        if (!dialogService.confirmDelete(productsTable.getScene().getWindow(), selected.getName())) return;

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

    private enum SortMode {
        NAME_ASC("По названию (А→Я)"),
        NAME_DESC("По названию (Я→А)"),
        PRICE_ASC("По цене (сначала дешёвые)"),
        PRICE_DESC("По цене (сначала дорогие)");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
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
}
