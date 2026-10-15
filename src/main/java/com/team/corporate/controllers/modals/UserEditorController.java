package com.team.corporate.controllers.modals;

import com.team.corporate.controllers.requests.UserEditorRequest;
import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;
import com.team.corporate.services.UsersService;
import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.ResultCarrier;
import com.team.corporate.utils.EntityValidation;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.UUID;

public class UserEditorController implements DataReceiver<UserEditorRequest>, ResultCarrier<UUID> {
    @FXML private TextField nameField;
    @FXML private ComboBox<UserRole> roleComboBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final UsersService usersService;

    private UserEditorRequest request;
    private UUID savedUserId;

    public UserEditorController(UsersService usersService) {
        this.usersService = usersService;
    }

    @Override
    public void receiveData(UserEditorRequest request) {
        this.request = request;
        if (request.isEditing()) {
            prefillForEdit(request.userId());
        }
    }

    @Override
    public UUID getResult() {
        return savedUserId;
    }

    @FXML
    public void initialize() {
        setupInputs();

        saveButton.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                nameField.textProperty(),
                roleComboBox.valueProperty()));
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
        UserRole role = roleComboBox.getValue();

        try {
            User saved = request.isEditing()
                    ? usersService.updateUser(request.userId(), name, role)
                    : usersService.createUser(name, role);
            savedUserId = saved.getId();
            close();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось сохранить пользователя." : e.getMessage());
        }
    }

    @FXML
    private void onCancelClick() {
        close();
    }

    private void prefillForEdit(UUID userId) {
        User user = usersService.getUser(userId)
                .orElseThrow(() -> new IllegalStateException("Редактируемый пользователь не найден. Возможно, он был удален."));
        nameField.setText(user.getUsername());
        roleComboBox.setValue(user.getUserRole());
    }

    private boolean isFormInvalid() {
        return nameField.getText().isBlank()
                || roleComboBox.getValue() == null;
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
        roleComboBox.setItems(FXCollections.observableArrayList(UserRole.values()));
        roleComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserRole userRole) {
                return userRole != null ? userRole.name() : "";
            }

            @Override
            public UserRole fromString(String string) {
                return null;
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Пользователь");
        alert.setHeaderText(null);
        alert.initOwner(saveButton.getScene().getWindow());
        alert.showAndWait();
    }
}
