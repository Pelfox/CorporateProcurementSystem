package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.session.UserSession;
import com.team.corporate.controllers.utils.TextLengthInput;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class LoginController {

    // Поля
    @FXML
    private TextField usernameField;
    @FXML
    private Label errorLabel;

    // Кнопка
    @FXML
    private Button loginBtn;

    private Function<String, Optional<UserSession>> authenticate;
    private Consumer<UserSession> onAuthenticated;

    @FXML
    public void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        TextLengthInput.configure(usernameField, null, errorLabel, "Имя пользователя");
        loginBtn.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !TextLengthInput.isValid(usernameField, true), usernameField.textProperty()));
        loginBtn.setDefaultButton(true);
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> errorLabel.setVisible(false));
    }

    public void setAuthentication(Function<String, Optional<UserSession>> authenticate,
                                  Consumer<UserSession> onAuthenticated) {
        this.authenticate = Objects.requireNonNull(authenticate);
        this.onAuthenticated = Objects.requireNonNull(onAuthenticated);
    }

    @FXML
    private void onLoginClick() {
        String username = TextLengthInput.text(usernameField).strip();
        if (!TextLengthInput.isValid(usernameField, true) || authenticate == null || onAuthenticated == null) {
            return;
        }
        try {
            authenticate.apply(username).ifPresentOrElse(onAuthenticated,
                    () -> showError("Пользователь с именем «" + username + "» не найден."));
        } catch (RuntimeException e) {
            showError(e.getMessage() == null ? "Не удалось выполнить вход." : e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
