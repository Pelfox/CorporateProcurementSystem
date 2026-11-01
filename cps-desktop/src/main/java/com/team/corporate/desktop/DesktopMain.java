package com.team.corporate.desktop;

import com.team.corporate.core.config.ApplicationServices;
import com.team.corporate.core.config.FileConfigurationProvider;
import com.team.corporate.desktop.controllers.ControllerFactory;
import com.team.corporate.desktop.controllers.pages.LoginController;
import com.team.corporate.desktop.controllers.pages.MainController;
import com.team.corporate.desktop.controllers.session.UserSession;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class DesktopMain extends Application {
    private final ControllerFactory controllerFactory = new ControllerFactory(this::services);
    private Stage stage;
    private ApplicationServices services;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        stage.setTitle("Система корпоративных закупок");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        showLogin();
        stage.show();
    }

    private void showMain(UserSession session) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = load(loader);
        MainController controller = loader.getController();
        controller.configure(controllerFactory, session, this::showLogin);
        setRoot(root);
    }

    private void showLogin() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login/LoginView.fxml"));
        Parent root = load(loader);
        LoginController controller = loader.getController();
        controller.setAuthentication(username -> services().users().login(username).map(UserSession::of), this::showMain);
        setRoot(root);
    }

    private Parent load(FXMLLoader loader) {
        try {
            loader.setControllerFactory(controllerFactory);
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить окно: " + loader.getLocation(), e);
        }
    }

    private void setRoot(Parent root) {
        if (stage.getScene() == null) {
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(java.util.Objects.requireNonNull(
                    getClass().getResource("/css/styles.css")).toExternalForm());
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(root);
        }
    }

    private ApplicationServices services() {
        if (services == null) {
            services = ApplicationServices.open(new FileConfigurationProvider().getConfiguration());
        }
        return services;
    }

    @Override
    public void stop() {
        if (services != null) {
            services.close();
        }
    }

    static void main(String[] args) {
        Application.launch(DesktopMain.class, args);
    }
}
