package com.team.corporate.controllers.navigation;

public enum Page {
    MY_ORDERS("/views/MyOrders.fxml"),
    HUB("/views/Services.fxml"),
    STATISTICS("/views/Statistics.fxml"),
    PROFILE("/views/Profile.fxml"),

    CATALOG("/views/Catalog.fxml"),
    CATEGORIES("/views/Categories.fxml"),
    USERS("/views/Users.fxml"),
    WAREHOUSES("/views/Warehouses.fxml");

    private final String fxmlPath;

    Page(String fxmlPath) { this.fxmlPath = fxmlPath; }

    public String getFxmlPath() { return fxmlPath; }

    public Page tabPage() {
        return switch (this) {
            case MY_ORDERS, HUB, STATISTICS, PROFILE -> this;
            case CATALOG, CATEGORIES, USERS, WAREHOUSES -> HUB;
        };
    }
}
