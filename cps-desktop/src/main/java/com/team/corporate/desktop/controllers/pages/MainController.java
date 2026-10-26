package com.team.corporate.desktop.controllers.pages;

import com.team.corporate.desktop.controllers.navigation.NavigationService;
import com.team.corporate.desktop.controllers.navigation.Page;
import com.team.corporate.desktop.controllers.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainController {
    @FXML
    private StackPane contentPane;
    @FXML
    private Button myOrdersNavBtn;
    @FXML
    private Button servicesNavBtn;
    @FXML
    private Button statisticsNavBtn;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label avatarInitialLabel;

    private NavigationService navigation;
    private Runnable onLogout;

    public void configure(Callback<Class<?>, Object> controllerFactory, UserSession session, Runnable onLogout) {
        this.onLogout = Objects.requireNonNull(onLogout);
        usernameLabel.setText(session.username());
        avatarInitialLabel.setText(session.username().substring(0, 1).toUpperCase(Locale.ROOT));
        navigation = new NavigationService(contentPane, controllerFactory, session, this::setActivePage);
        navigation.navigate(Page.MY_ORDERS);
    }

    @FXML
    private void onMyOrdersClick() {
        navigation.navigate(Page.MY_ORDERS);
    }

    @FXML
    private void onServicesClick() {
        navigation.navigate(Page.SERVICES);
    }

    @FXML
    private void onStatisticsClick() {
        navigation.navigate(Page.STATISTICS);
    }

    @FXML
    private void onLogoutClick() {
        onLogout.run();
    }

    private void setActivePage(Page page) {
        Map.of(Page.MY_ORDERS, myOrdersNavBtn, Page.SERVICES, servicesNavBtn,
                Page.STATISTICS, statisticsNavBtn).forEach((tab, button) -> {
            button.getStyleClass().remove("active");
            if (tab == page.tab()) button.getStyleClass().add("active");
        });
    }
}
