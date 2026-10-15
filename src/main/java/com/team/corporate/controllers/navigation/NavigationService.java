package com.team.corporate.controllers.navigation;

import com.team.corporate.controllers.dialog.DataReceiver;
import com.team.corporate.controllers.pages.PageController;
import com.team.corporate.controllers.pages.ServicesController;
import com.team.corporate.controllers.session.UserSession;
import com.team.corporate.controllers.utils.UiErrors;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

public final class NavigationService {
    private final StackPane contentArea;
    private final Callback<Class<?>, Object> controllerFactory;
    private final UserSession session;
    private final Consumer<Page> onPageChanged;
    private final Deque<Page> history = new ArrayDeque<>();
    private Page currentPage;

    public NavigationService(StackPane contentArea, Callback<Class<?>, Object> controllerFactory,
                             UserSession session, Consumer<Page> onPageChanged) {
        this.contentArea = contentArea;
        this.controllerFactory = controllerFactory;
        this.session = Objects.requireNonNull(session);
        this.onPageChanged = onPageChanged;
    }

    public void navigate(Page page) {
        Objects.requireNonNull(page);
        if (!page.isAccessibleTo(session.role())) {
            UiErrors.show(contentArea.getScene() == null ? null : contentArea.getScene().getWindow(),
                    "Нет доступа", new IllegalStateException("Раздел доступен только менеджеру."));
            return;
        }
        if (page == currentPage) return;
        if (!showPage(page)) return;
        if (currentPage != null) history.push(currentPage);
        currentPage = page;
    }

    public void goBack() {
        Page previous = history.peek();
        if (previous == null) {
            navigate(Page.SERVICES);
            return;
        }
        if (!showPage(previous)) return;
        history.pop();
        currentPage = previous;
    }

    @SuppressWarnings("unchecked")
    private boolean showPage(Page page) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource(page.getFxmlPath()), "Страница не найдена: " + page.getFxmlPath()));
            loader.setControllerFactory(controllerFactory);
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof PageController pageController) pageController.setOnBack(this::goBack);
            if (controller instanceof ServicesController servicesController)
                servicesController.setOnNavigate(this::navigate);
            if (controller instanceof DataReceiver<?> receiver)
                ((DataReceiver<UserSession>) receiver).receiveData(session);
            contentArea.getChildren().setAll(view);
            onPageChanged.accept(page);
            return true;
        } catch (IOException | RuntimeException e) {
            UiErrors.show(contentArea.getScene() == null ? null : contentArea.getScene().getWindow(),
                    "Не удалось открыть страницу", e);
            return false;
        }
    }
}
