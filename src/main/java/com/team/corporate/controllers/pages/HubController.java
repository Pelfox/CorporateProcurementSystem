package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.navigation.Page;
import com.team.corporate.controllers.session.UserSession;
import com.team.corporate.entities.UserRole;
import com.team.corporate.services.CsvExportService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class HubController implements DataReceiver<UserSession> {
    private final Consumer<Page> onNavigate;
    private final CsvExportService csvExportService;
    @FXML
    private Button catalogCard;
    @FXML
    private Button ordersCard;
    @FXML
    private Button categoriesCard;
    @FXML
    private Button usersCard;
    @FXML
    private Button warehousesCard;
    @FXML
    private Button statisticsCard;
    @FXML
    private Button exportCard;
    private UserSession session;

    public HubController(Consumer<Page> onNavigate, CsvExportService csvExportService) {
        this.onNavigate = onNavigate;
        this.csvExportService = csvExportService;
    }

    @FXML
    public void initialize() {
        catalogCard.setOnAction(event -> onNavigate.accept(Page.PRODUCTS));
        ordersCard.setOnAction(event -> onNavigate.accept(Page.MY_ORDERS));
        categoriesCard.setOnAction(event -> onNavigate.accept(Page.CATEGORIES));
        usersCard.setOnAction(event -> onNavigate.accept(Page.USERS));
        warehousesCard.setOnAction(event -> onNavigate.accept(Page.WAREHOUSES));
        statisticsCard.setOnAction(event -> onNavigate.accept(Page.STATISTICS));
        exportCard.setOnAction(event -> onExportClick());
    }

    @Override
    public void receiveData(UserSession session) {
        this.session = session;
        boolean manager = session.role() == UserRole.MANAGER;
        usersCard.setVisible(manager);
        usersCard.setManaged(manager);
        exportCard.setVisible(manager);
        exportCard.setManaged(manager);
    }

    private void onExportClick() {
        if (session == null) {
            throw new IllegalStateException("Сессия не передана на страницу (payload отсутствует).");
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Папка для экспорта CSV");
        File directory = chooser.showDialog(exportCard.getScene().getWindow());
        if (directory == null) {
            return;
        }

        try {
            Path destination = csvExportService.exportAll(session.userId(), directory.toPath());
            showAlert(Alert.AlertType.INFORMATION, "Данные выгружены в папку:\n" + destination);
        } catch (IOException | RuntimeException e) {
            showAlert(Alert.AlertType.WARNING,
                    e.getMessage() == null ? "Не удалось выполнить экспорт данных." : e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle("Экспорт данных");
        alert.setHeaderText(null);
        alert.initOwner(exportCard.getScene().getWindow());
        alert.showAndWait();
    }
}
