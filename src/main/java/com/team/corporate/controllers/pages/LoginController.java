package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.session.UserSession;
import com.team.corporate.services.UsersService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private final UsersService usersService;
    private final Consumer<UserSession> onAuthenticated;

    public LoginController(UsersService usersService, Consumer<UserSession> onAuthenticated) {
        this.usersService = usersService;
        this.onAuthenticated = onAuthenticated;
    }

    @FXML
    public void initialize() {
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        errorLabel.setVisible(false);
        loginButton.disableProperty().bind(usernameField.textProperty().isEmpty());
        usernameField.setOnAction(event -> onLoginClick());
        usernameField.textProperty().addListener((obs, old, value) -> errorLabel.setVisible(false));
    }

    @FXML
    private void onLoginClick() {
        String username = usernameField.getText().strip();
        if (username.isEmpty()) {
            return;
        }
        try {
            usersService.login(username).ifPresentOrElse(
                    user -> onAuthenticated.accept(UserSession.of(user)),
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