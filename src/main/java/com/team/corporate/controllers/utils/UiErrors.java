package com.team.corporate.controllers.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import org.slf4j.LoggerFactory;

public final class UiErrors {
    private UiErrors() {
    }

    public static void show(Window owner, String title, Throwable error) {
        LoggerFactory.getLogger(UiErrors.class).error(title, error);
        String message = error instanceof IllegalArgumentException || error instanceof IllegalStateException
                ? error.getMessage() : null;
        Alert alert = new Alert(Alert.AlertType.WARNING,
                message == null || message.isBlank() ? "Не удалось выполнить операцию. Повторите попытку." : message,
                ButtonType.OK);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
