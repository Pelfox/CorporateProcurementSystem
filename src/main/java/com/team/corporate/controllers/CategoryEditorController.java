package com.team.corporate.controllers;

import com.team.corporate.controllers.requests.CategoryEditorRequest;
import com.team.corporate.entities.Category;
import com.team.corporate.entities.Product;
import com.team.corporate.services.CategoriesService;
import com.team.corporate.services.ui.DataReceiver;
import com.team.corporate.services.ui.ResultCarrier;
import com.team.corporate.utils.EntityValidation;
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

public class CategoryEditorController implements DataReceiver<CategoryEditorRequest>, ResultCarrier<UUID> {
    @FXML private TextField nameField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final CategoriesService categoriesService;

    private CategoryEditorRequest request;
    private UUID savedCategoryId;

    public CategoryEditorController(CategoriesService categoriesService) {
        this.categoriesService = categoriesService;
    }

    @Override
    public void receiveData(CategoryEditorRequest request) {
        this.request = request;
        if (request.isEditing()) {
            prefillForEdit(request.categoryId());
        }
    }

    @Override
    public UUID getResult() {
        return savedCategoryId;
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

        try {
            Category saved = request.isEditing()
                    ? categoriesService.updateCategory(request.categoryId(), name)
                    : categoriesService.createCategory(name);
            savedCategoryId = saved.getId();
            close();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось сохранить категорию." : e.getMessage());
        }
    }

    @FXML
    private void onCancelClick() {
        close();
    }

    private void prefillForEdit(UUID categoryId) {
        Category category = categoriesService.getCategory(categoryId)
                .orElseThrow(() -> new IllegalStateException("Редактируемая категория не найдена. Возможно, она была удалена."));
        nameField.setText(category.getName());
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
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Категория");
        alert.setHeaderText(null);
        alert.initOwner(saveButton.getScene().getWindow());
        alert.showAndWait();
    }
}
