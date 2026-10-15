package com.team.corporate.controllers.navigation;

import com.team.corporate.controllers.pages.HeaderController;
import com.team.corporate.controllers.session.UserSession;
import com.team.corporate.controllers.dialog.DataReceiver;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class NavigationService {
    private final Stage primaryStage;
    private Callback<Class<?>, Object> controllerFactory;

    private HeaderController headerController;
    private StackPane contentArea;
    private UserSession session;

    public NavigationService(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setControllerFactory(Callback<Class<?>, Object> controllerFactory) {
        this.controllerFactory = controllerFactory;
    }

    public void start(UserSession session) {
        this.session = session;
        primaryStage.setScene(buildMainScene());
        navigate(Page.HUB);
    }

    private Scene buildMainScene() {
        try {
            FXMLLoader headerLoader = new FXMLLoader(getClass().getResource("/views/Header.fxml"));
            headerLoader.setControllerFactory(controllerFactory);
            Parent header = headerLoader.load();
            headerController = headerLoader.getController();
            headerController.setOnNavigate(this::navigate);

            BorderPane root = new BorderPane();
            root.setTop(header);
            contentArea = new StackPane();
            root.setCenter(contentArea);

            Scene scene = new Scene(root, 1200, 750);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/views/styles.css")).toExternalForm());
            return scene;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось построить каркас приложения.", e);
        }
    }

    public void navigate(Page page) {
        try {
            URL location = getClass().getResource(page.getFxmlPath());
            if (location == null) {
                throw new IllegalStateException("Указанный FXML-файл не найден: " + page.getFxmlPath());
            }
            FXMLLoader loader = new FXMLLoader(location);
            loader.setControllerFactory(controllerFactory);
            Parent view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DataReceiver<?> receiver) {
                ((DataReceiver<UserSession>) receiver).receiveData(session);
            }

            contentArea.getChildren().setAll(view);
            headerController.setActivePage(page);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось открыть страницу: " + page, e);
        }
    }
}
