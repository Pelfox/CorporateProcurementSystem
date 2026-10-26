package com.team.corporate.desktop.controllers.modals;

import com.team.corporate.desktop.controllers.dialog.DataReceiver;
import com.team.corporate.desktop.controllers.dialog.ResultCarrier;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ConfirmDeleteController implements DataReceiver<String>, ResultCarrier<Boolean> {
    private boolean confirmed;

    @FXML
    private Label confirmEmoji;
    @FXML
    private Label confirmText;
    @FXML
    private Label confirmWarning;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button deleteBtn;

    @FXML
    public void initialize() {
        confirmEmoji.setText("⚠");
    }

    /**
     * Текст подтверждения из вызывающего кода
     */
    public void setEntityName(String entityName) {
        confirmText.setText("Вы уверены, что хотите удалить\n\"" + entityName + "\"?");
    }

    @Override
    public void receiveData(String name) {
        setEntityName(name);
    }

    @Override
    public Boolean getResult() {
        return confirmed;
    }

    @FXML
    private void onDeleteClick() {
        confirmed = true;
        deleteBtn.getScene().getWindow().hide();
    }

    @FXML
    private void onCancelClick() {
        cancelBtn.getScene().getWindow().hide();
    }
}
