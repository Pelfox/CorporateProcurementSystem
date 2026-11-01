package com.team.corporate.desktop.controllers.modals;

import com.team.corporate.desktop.controllers.dialog.DataReceiver;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ErrorController implements DataReceiver<String> {

    @FXML
    private Label errorEmoji;
    @FXML
    private Label errorText;
    @FXML
    private Button okBtn;

    @FXML
    public void initialize() {
        errorEmoji.setText("❌");
    }

    /**
     * Текст ошибки из вызывающего кода
     */
    public void setErrorMessage(String message) {
        errorText.setText(message);
    }

    @Override
    public void receiveData(String message) {
        setErrorMessage(message);
    }

    @FXML
    private void onCloseClick() {
        okBtn.getScene().getWindow().hide();
    }
}
