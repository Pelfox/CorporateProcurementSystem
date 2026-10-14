package com.team.corporate;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public abstract class BaseControllerTest extends BaseTest {
    protected Parent viewRoot;
    protected Object controller;

    @BeforeEach
    void resetController() {
        viewRoot = null;
        controller = null;
    }

    protected final <T> T loadController(String resourcePath, Class<T> controllerType) throws IOException {
        URL resource = Objects.requireNonNull(
                getClass().getResource(resourcePath), "FXML resource not found: " + resourcePath);
        FXMLLoader loader = new FXMLLoader(resource);
        viewRoot = loader.load();
        controller = loader.getController();
        return controllerType.cast(controller);
    }

    protected final Parent loadView(String resourcePath) throws IOException {
        loadController(resourcePath, Object.class);
        return viewRoot;
    }
}
