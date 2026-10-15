package com.team.corporate.controllers.modals;

import com.team.corporate.controllers.dialog.DataReceiver;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SuccessController implements DataReceiver<String> {

    @FXML
    private Label successEmoji;
    @FXML
    private Label successText;
    @FXML
    private Button okBtn;

    @FXML
    public void initialize() {
        successEmoji.setText("✅");
    }

    /**
     * Текст успеха из вызывающего кода
     */
    public void setSuccessMessage(String message) {
        successText.setText(message);
    }

    @Override
    public void receiveData(String message) {
        setSuccessMessage(message);
    }

    @FXML
    private void onCloseClick() {
        okBtn.getScene().getWindow().hide();
    }
}
