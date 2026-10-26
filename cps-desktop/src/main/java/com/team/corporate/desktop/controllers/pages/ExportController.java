package com.team.corporate.desktop.controllers.pages;

import com.team.corporate.desktop.controllers.dialog.DataReceiver;
import com.team.corporate.desktop.controllers.session.UserSession;
import com.team.corporate.core.services.CsvExportService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;

public class ExportController extends PageController implements DataReceiver<UserSession> {
    private final CsvExportService exporter;
    @FXML
    private VBox exportAllCard;
    @FXML
    private Label exportEmoji;
    private UserSession session;

    public ExportController(CsvExportService exporter) {
        this.exporter = exporter;
    }

    @FXML
    public void initialize() {
        exportEmoji.setText("💾");
    }

    @Override
    public void receiveData(UserSession session) {
        this.session = session;
    }

    @FXML
    private void onExportClick() {
        if (session == null || session.role() != com.team.corporate.entities.UserRole.MANAGER) return;
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Папка для экспорта CSV");
        File folder = chooser.showDialog(exportAllCard.getScene().getWindow());
        if (folder == null) return;
        try {
            var destination = exporter.exportAll(session.userId(), folder.toPath());
            showMessage(Alert.AlertType.INFORMATION, "Данные выгружены в папку:\n" + destination);
        } catch (IOException | RuntimeException e) {
            showMessage(Alert.AlertType.WARNING, e.getMessage() == null ? "Не удалось выгрузить данные." : e.getMessage());
        }
    }

    @FXML
    private void onExportKeyPressed(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER || event.getCode() == javafx.scene.input.KeyCode.SPACE) {
            onExportClick();
            event.consume();
        }
    }

    private void showMessage(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.initOwner(exportAllCard.getScene().getWindow());
        alert.setTitle("Экспорт данных");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
