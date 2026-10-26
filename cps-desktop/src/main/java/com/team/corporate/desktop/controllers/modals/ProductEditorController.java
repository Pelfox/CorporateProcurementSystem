package com.team.corporate.desktop.controllers.modals;

import com.team.corporate.desktop.controllers.dialog.DataReceiver;
import com.team.corporate.desktop.controllers.dialog.ResultCarrier;
import com.team.corporate.desktop.controllers.requests.ProductEditorRequest;
import com.team.corporate.desktop.controllers.utils.MoneyInput;
import com.team.corporate.desktop.controllers.utils.TextLengthInput;
import com.team.corporate.core.entities.Category;
import com.team.corporate.core.entities.Product;
import com.team.corporate.core.services.CategoriesService;
import com.team.corporate.core.services.ProductsService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class ProductEditorController implements DataReceiver<ProductEditorRequest>, ResultCarrier<UUID> {
    private static final Pattern SKU_INPUT = Pattern.compile("[A-Z0-9-]{0,40}");
    private static final Pattern SKU_FORMAT = Pattern.compile("[A-Z0-9]{3,10}(-[A-Z0-9]{3,10})*");
    private final ProductsService productsService;
    private final CategoriesService categoriesService;
    @FXML
    private TextField nameField;
    @FXML
    private TextField skuField;
    @FXML
    private Label nameError;
    @FXML
    private ComboBox<Category> categoryCombo;
    @FXML
    private TextField priceField;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;
    private ProductEditorRequest request;
    private UUID savedProductId;

    public ProductEditorController(ProductsService productsService, CategoriesService categoriesService) {
        this.productsService = productsService;
        this.categoriesService = categoriesService;
    }

    private static UnaryOperator<TextFormatter.Change> skuFilter() {
        return change -> {
            if (change.getText() != null && !change.getText().isEmpty()) {
                change.setText(change.getText().toUpperCase(java.util.Locale.ROOT));
            }
            return SKU_INPUT.matcher(TextLengthInput.proposedText(change)).matches() ? change : null;
        };
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

        saveBtn.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                nameField.textProperty(),
                skuField.textProperty(),
                categoryCombo.valueProperty(),
                priceField.textProperty()));
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
        String sku = TextLengthInput.text(skuField).strip();
        UUID categoryId = categoryCombo.getValue().getId();
        BigDecimal price = MoneyInput.value(priceField);

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
        TextLengthInput.configure(nameField, null, nameError, "Название товара");
        MoneyInput.configure(priceField);
        ObservableList<Category> categories =
                FXCollections.observableArrayList(categoriesService.getAll());
        categories.sort(Comparator.comparing(Category::getName));
        categoryCombo.setItems(categories);
        categoryCombo.setConverter(new StringConverter<>() {
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
        priceField.setText(product.getPrice().toPlainString().replace('.', ','));
        if (product.getCategory() != null) {
            UUID categoryId = product.getCategory().getId();
            categoryCombo.getItems().stream()
                    .filter(category -> category.getId().equals(categoryId))
                    .findFirst()
                    .ifPresent(categoryCombo::setValue);
        }
    }

    private boolean isFormInvalid() {
        return !TextLengthInput.isValid(nameField, true)
                || !TextLengthInput.isValid(skuField, true)
                || skuField.getLength() > 40 || !SKU_FORMAT.matcher(skuField.getText()).matches()
                || categoryCombo.getValue() == null || !categoryCombo.getItems().contains(categoryCombo.getValue())
                || MoneyInput.value(priceField) == null
                || MoneyInput.value(priceField).signum() <= 0;
    }

    private void close() {
        Window window = saveBtn.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Товар");
        alert.setHeaderText(null);
        alert.initOwner(saveBtn.getScene().getWindow());
        alert.showAndWait();
    }
}
