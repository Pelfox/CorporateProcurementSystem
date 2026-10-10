package com.team.corporate.services.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class DialogService {
    private final Callback<Class<?>, Object> controllerFactory;

    public DialogService(Callback<Class<?>, Object> controllerFactory) {
        this.controllerFactory = controllerFactory;
    }

    public <R> Optional<R> showModal(ViewType viewType, Window owner) {
        // Перегрузка модалки без передаваемых данных
        return showModal(viewType, owner, null);
    }

    @SuppressWarnings("unchecked")
    public <T, R> Optional<R> showModal(ViewType viewType, Window owner, T payload) {
        // Перегрузка модалки с предварительной передачей данных
        try {
            URL location = getClass().getResource(viewType.getFxmlPath());
            if (location == null) {
                throw new IllegalStateException("Указанный FXML-файл не найден: " + viewType.getFxmlPath());
            }

            FXMLLoader loader = new FXMLLoader(location);
            loader.setControllerFactory(controllerFactory);
            Parent root = loader.load();

            var controller = loader.getController();
            if (controller instanceof DataReceiver<?> receiver) {
                if (payload == null) {
                    throw new IllegalStateException("Окно " + viewType.getTitle()
                            + " требует входные данные, однако в payload ничего не передано.");
                }
                ((DataReceiver<T>) receiver).receiveData(payload);
            }

            Scene scene = new Scene(root);
            if (owner != null && owner.getScene() != null) {
                scene.getStylesheets().addAll(owner.getScene().getStylesheets());
            }

            Stage stage = new Stage();
            stage.setTitle(viewType.getTitle());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setScene(scene);
            stage.showAndWait();

            if (controller instanceof ResultCarrier<?> carrier) {
                return Optional.ofNullable(((ResultCarrier<R>) carrier).getResult());
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить окно: " + viewType, e);
        }
    }
}
