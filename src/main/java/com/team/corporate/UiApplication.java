package com.team.corporate;

import com.team.corporate.config.ApplicationServices;
import com.team.corporate.config.FileConfigurationProvider;
import com.team.corporate.controllers.dialog.DialogService;
import com.team.corporate.controllers.modals.*;
import com.team.corporate.controllers.navigation.NavigationService;
import com.team.corporate.controllers.pages.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.jline.utils.Log;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class UiApplication extends Application {
    private ApplicationServices app;
    private NavigationService navigationService;
    private DialogService dialogService;

    @Override
    public void init() throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            Log.error("Необработанная ошибка в потоке " + thread.getName(), error);
            if (Platform.isFxApplicationThread()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "Произошла непредвиденная ошибка. Подробности — в журнале.", ButtonType.OK);
                    alert.setHeaderText(null);
                    alert.showAndWait();
                });
            }
        });
        app = ApplicationServices.open(new FileConfigurationProvider().getConfiguration());
    }

    @Override
    public void start(Stage stage) throws IOException {
        navigationService = new NavigationService(stage);
        dialogService = new DialogService(this::createController);
        navigationService.setControllerFactory(this::createController);

        URL location = getClass().getResource("/views/Login.fxml");
        if (location == null) {
            throw new IllegalStateException("Указанный FXML-файл не найден: /views/Login.fxml");
        }
        FXMLLoader loader = new FXMLLoader(location);
        loader.setControllerFactory(this::createController);
        Parent root = loader.load();

        Scene scene = new Scene(root, 420, 340);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/views/styles.css")).toExternalForm());
        stage.setTitle("Система управления корпоративными закупками");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (app != null) {
            app.close();
        }
    }

    private Object createController(Class<?> clazz) {
        if (clazz == LoginController.class) {
            return new LoginController(app.users(), navigationService::start);
        }
        if (clazz == HeaderController.class) {
            return new HeaderController();
        }
        if (clazz == HubController.class) {
            return new HubController(navigationService::navigate, app.csvExport());
        }
        if (clazz == MyOrdersController.class) {
            return new MyOrdersController(app.orders(), app.items());
        }
        if (clazz == CatalogController.class) {
            return new CatalogController(app.products(), app.categories(), dialogService);
        }
        if (clazz == CategoriesController.class) {
            return new CategoriesController(app.categories(), app.products(), dialogService);
        }
        if (clazz == WarehousesController.class) {
            return new WarehousesController(app.warehouses(), app.inventories(), app.products(), dialogService);
        }
        if (clazz == StatisticsController.class) {
            return new StatisticsController(app.statistics());
        }

        if (clazz == OrderEditorController.class) {
            return new OrderEditorController(app.orders(), app.products(), app.items());
        }
        if (clazz == ProductEditorController.class) {
            return new ProductEditorController(app.products(), app.categories());
        }
        if (clazz == CategoryEditorController.class) {
            return new CategoryEditorController(app.categories());
        }
        if (clazz == WarehouseEditorController.class) {
            return new WarehouseEditorController(app.warehouses());
        }
        if (clazz == StockEditorController.class) {
            return new StockEditorController(app.inventories(), app.warehouses(), app.products());
        }
        if (clazz == UserEditorController.class) {
            return new UserEditorController(app.users());
        }
        if (clazz == ChangeOrderStatusController.class) {
            return new ChangeOrderStatusController(app.orders());
        }

        throw new IllegalStateException("Фабрика контроллеров не настроена для: " + clazz.getName());
    }
}