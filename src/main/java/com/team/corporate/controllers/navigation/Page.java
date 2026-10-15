package com.team.corporate.controllers.navigation;

import com.team.corporate.entities.UserRole;

public enum Page {
    MY_ORDERS("/fxml/orders/MyOrdersView.fxml"),
    SERVICES("/fxml/main/ServicesView.fxml"),
    STATISTICS("/fxml/statistics/StatisticsView.fxml"),
    PRODUCTS("/fxml/products/ProductsView.fxml"),
    ORDERS("/fxml/orders/OrdersView.fxml"),
    CATEGORIES("/fxml/categories/CategoriesView.fxml"),
    WAREHOUSES("/fxml/warehouses/WarehousesView.fxml"),
    EXPORT("/fxml/export/ExportView.fxml"),
    USERS("/fxml/users/UsersView.fxml");

    private final String fxmlPath;

    Page(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public boolean isAccessibleTo(UserRole role) {
        return (this != USERS && this != EXPORT) || role == UserRole.MANAGER;
    }

    public Page tab() {
        return switch (this) {
            case MY_ORDERS, STATISTICS -> this;
            default -> SERVICES;
        };
    }
}
