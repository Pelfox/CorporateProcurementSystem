package com.team.corporate.controllers.modals;

import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.ResultCarrier;
import com.team.corporate.controllers.requests.CategoryEditorRequest;
import com.team.corporate.controllers.utils.TextLengthInput;
import com.team.corporate.entities.Category;
import com.team.corporate.services.CategoriesService;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.UUID;

public class CategoryEditorController implements DataReceiver<CategoryEditorRequest>, ResultCarrier<UUID> {
    private final CategoriesService categoriesService;
    @FXML
    private TextField nameField;
    @FXML
    private Label nameError;
    @FXML
    private Label nameLengthLabel;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;
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

        saveBtn.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                nameField.textProperty()));
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
        return !TextLengthInput.isValid(nameField, true);
    }

    private void close() {
        Window window = saveBtn.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void setupInputs() {
        TextLengthInput.configure(nameField, nameLengthLabel, nameError, "Название категории");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Категория");
        alert.setHeaderText(null);
        alert.initOwner(saveBtn.getScene().getWindow());
        alert.showAndWait();
    }
}
