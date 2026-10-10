package com.team.corporate.services.ui;

public enum ViewType {
    CREATE_ORDER("/views/CreateOrder.fxml", "Оформление нового заказа"),
    CREATE_PRODUCT("/views/ProductEditor.fxml", "Новый товар"),
    EDIT_ORDER("/views/ProductEditor.fxml", "Редактирование товара"),
    CREATE_CATEGORY("/views/CategoryEditor.fxml", "Новая категория"),
    EDIT_CATEGORY("/views/CategoryEditor.fxml", "Редактирование категории");


    private final String fxmlPath;
    private final String title;

    ViewType(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    public String getFxmlPath() { return fxmlPath; }
    public String getTitle() { return title; }
}
