package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.navigation.Page;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class HeaderController {
    @FXML
    private Button myOrdersButton, hubButton, statisticsButton, profileButton, logoutButton;

    private Consumer<Page> onNavigate = page -> {};
    private final Map<Page, Button> tabs = new EnumMap<>(Page.class);

    public void setOnNavigate(Consumer<Page> onNavigate) { this.onNavigate = onNavigate; }

    @FXML
    public void initialize() {
        tabs.put(Page.MY_ORDERS, myOrdersButton);
        tabs.put(Page.HUB, hubButton);
        tabs.put(Page.STATISTICS, statisticsButton);
        tabs.put(Page.PROFILE, profileButton);

        myOrdersButton.setOnAction(e -> onNavigate.accept(Page.MY_ORDERS));
        hubButton.setOnAction(e -> onNavigate.accept(Page.HUB));
        statisticsButton.setOnAction(e -> onNavigate.accept(Page.STATISTICS));
        profileButton.setOnAction(e -> onNavigate.accept(Page.PROFILE));
    }

    public void setActivePage(Page page) {
        tabs.forEach((p, button) -> button.getStyleClass().remove("active"));
        Button active = tabs.get(page.tabPage());
        if (active != null) {
            active.getStyleClass().add("active");
        }
    }
}