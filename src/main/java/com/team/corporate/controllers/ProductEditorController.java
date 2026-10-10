package com.team.corporate.controllers;

import com.team.corporate.controllers.requests.ProductEditorRequest;
import com.team.corporate.entities.Category;
import com.team.corporate.entities.Product;
import com.team.corporate.services.CategoriesService;
import com.team.corporate.services.ProductsService;
import com.team.corporate.services.ui.DataReceiver;
import com.team.corporate.services.ui.ResultCarrier;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class ProductEditorController implements DataReceiver<ProductEditorRequest>, ResultCarrier<UUID> {
    @FXML private TextField nameField;
    @FXML private TextField skuField;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private TextField priceField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final ProductsService productsService;
    private final CategoriesService categoriesService;

    private static final Pattern SKU_INPUT = Pattern.compile("[A-Z0-9-]{0,40}");
    private static final Pattern SKU_FORMAT = Pattern.compile("[A-Z0-9]{3,10}(-[A-Z0-9]{3,10})*");
    private static final Pattern MONEY_INPUT = Pattern.compile("[0-9]{0,8}([.,][0-9]{0,2})?");

    private final TextFormatter<BigDecimal> priceFormatter =
            new TextFormatter<>(moneyConverter(), null, moneyFilter());

    private ProductEditorRequest request;
    private UUID savedProductId;

    public ProductEditorController(ProductsService productsService, CategoriesService categoriesService) {
        this.productsService = productsService;
        this.categoriesService = categoriesService;
    }

    @Override
    public void receiveData(ProductEditorRequest request) {
        this.request = request;
        if (request.isEditing()) {
            prefillForEdit(request.productId());
        }
    }

    @Override
    public UUID getResult() {
        return savedProductId;
    }

    @FXML
    public void initialize() {
        setupInputs();

        saveButton.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                nameField.textProperty(),
                skuField.textProperty(),
                categoryComboBox.valueProperty(),
                priceFormatter.valueProperty()));
    }

    @FXML
    private void onSaveClick() {
        if (request == null) {
            throw new IllegalStateException("Режим окна не инициализирован (payload не передан).");
        }

        priceField.commitValue();
        if (isFormInvalid()) {
            return;
        }

        String name = nameField.getText().strip();
        String sku = skuField.getText().strip();
        UUID categoryId = categoryComboBox.getValue().getId();
        BigDecimal price = priceFormatter.getValue();

        try {
            Product saved = request.isEditing()
                    ? productsService.updateProduct(request.productId(), name, sku, categoryId, price)
                    : productsService.createProduct(name, sku, categoryId, price);
            savedProductId = saved.getId();
            close();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось сохранить товар." : e.getMessage());
        }
    }

    @FXML
    private void onCancelClick() {
        close();
    }

    private void setupInputs() {
        skuField.setTextFormatter(new TextFormatter<>(skuFilter()));
        priceField.setTextFormatter(priceFormatter);
        ObservableList<Category> categories =
                FXCollections.observableArrayList(categoriesService.getAll());
        categories.sort(Comparator.comparing(Category::getName));
        categoryComboBox.setItems(categories);
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category != null ? category.getName() : "";
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        });
    }

    private void prefillForEdit(UUID productId) {
        Product product = productsService.getProduct(productId)
                .orElseThrow(() -> new IllegalStateException("Редактируемый товар не найден. Возможно, он был удалён."));
        nameField.setText(product.getName());
        skuField.setText(product.getSku());
        priceFormatter.setValue(product.getPrice());
        if (product.getCategory() != null) {
            UUID categoryId = product.getCategory().getId();
            categoryComboBox.getItems().stream()
                    .filter(category -> category.getId().equals(categoryId))
                    .findFirst()
                    .ifPresent(categoryComboBox::setValue);
        }
    }

    private boolean isFormInvalid() {
        return nameField.getText().isBlank()
                || !SKU_FORMAT.matcher(skuField.getText()).matches()
                || categoryComboBox.getValue() == null
                || priceFormatter.getValue() == null
                || priceFormatter.getValue().signum() <= 0;
    }

    private static UnaryOperator<TextFormatter.Change> skuFilter() {
        return change -> {
            if (change.getText() != null && !change.getText().isEmpty()) {
                change.setText(change.getText().toUpperCase());
            }
            return SKU_INPUT.matcher(change.getControlNewText()).matches() ? change : null;
        };
    }

    private static UnaryOperator<TextFormatter.Change> moneyFilter() {
        return change -> MONEY_INPUT.matcher(change.getControlNewText()).matches() ? change : null;
    }

    private static StringConverter<BigDecimal> moneyConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(BigDecimal value) {
                return value == null ? ""
                        : value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
            }

            @Override
            public BigDecimal fromString(String text) {
                String cleaned = text == null ? "" : text.strip();
                if (cleaned.endsWith(",") || cleaned.endsWith(".")) {
                    cleaned = cleaned.substring(0, cleaned.length() - 1);
                }
                if (cleaned.isEmpty()) {
                    return null;
                }
                try {
                    return new BigDecimal(cleaned.replace(',', '.'));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        };
    }

    private void close() {
        Window window = saveButton.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Товар");
        alert.setHeaderText(null);
        alert.initOwner(saveButton.getScene().getWindow());
        alert.showAndWait();
    }
}
