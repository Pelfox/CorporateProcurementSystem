package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.navigation.Page;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class HeaderController {
    private final Map<Page, Button> tabs = new EnumMap<>(Page.class);
    @FXML
    private Button myOrdersButton, hubButton, statisticsButton, profileButton, logoutButton;
    private Consumer<Page> onNavigate = page -> {
    };

    public void setOnNavigate(Consumer<Page> onNavigate) {
        this.onNavigate = onNavigate;
    }

    @FXML
    public void initialize() {
        tabs.put(Page.MY_ORDERS, myOrdersButton);
        tabs.put(Page.SERVICES, hubButton);
        tabs.put(Page.STATISTICS, statisticsButton);

        myOrdersButton.setOnAction(e -> onNavigate.accept(Page.MY_ORDERS));
        hubButton.setOnAction(e -> onNavigate.accept(Page.SERVICES));
        statisticsButton.setOnAction(e -> onNavigate.accept(Page.STATISTICS));
    }

    public void setActivePage(Page page) {
        tabs.forEach((p, button) -> button.getStyleClass().remove("active"));
        Button active = tabs.get(page.tab());
        if (active != null) {
            active.getStyleClass().add("active");
        }
    }
}
