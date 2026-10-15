package com.team.corporate.controllers.modals;

import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.dialog.ResultCarrier;
import com.team.corporate.controllers.requests.UserEditorRequest;
import com.team.corporate.controllers.utils.TextLengthInput;
import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;
import com.team.corporate.services.UsersService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.UUID;

public class UserEditorController implements DataReceiver<UserEditorRequest>, ResultCarrier<UUID> {
    private final UsersService usersService;
    @FXML
    private TextField usernameField;
    @FXML
    private Label usernameLengthLabel;
    @FXML
    private Label usernameError;
    @FXML
    private ComboBox<UserRole> roleCombo;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;
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

        saveBtn.disableProperty().bind(Bindings.createBooleanBinding(
                this::isFormInvalid,
                usernameField.textProperty(),
                roleCombo.valueProperty()));
    }

    @FXML
    private void onSaveClick() {
        if (request == null) {
            return;
        }

        if (isFormInvalid()) {
            return;
        }

        String name = TextLengthInput.text(usernameField).strip();
        UserRole role = roleCombo.getValue();

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
        usernameField.setText(user.getUsername());
        roleCombo.setValue(user.getUserRole());
    }

    private boolean isFormInvalid() {
        return !TextLengthInput.isValid(usernameField, true)
                || roleCombo.getValue() == null;
    }

    private void close() {
        Window window = saveBtn.getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.close();
        }
    }

    private void setupInputs() {
        TextLengthInput.configure(usernameField, usernameLengthLabel, usernameError, "Имя пользователя");
        roleCombo.setItems(FXCollections.observableArrayList(UserRole.values()));
        roleCombo.setConverter(new StringConverter<>() {
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
        alert.initOwner(saveBtn.getScene().getWindow());
        alert.showAndWait();
    }
}
