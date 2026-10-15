package com.team.corporate.controllers.dialog;

public enum ViewType {
    CREATE_ORDER("/views/OrderEditor.fxml", "Новый заказ"),
    EDIT_ORDER("/views/OrderEditor.fxml", "Редактирование заказа"),
    CREATE_PRODUCT("/views/ProductEditor.fxml", "Новый товар"),
    EDIT_PRODUCT("/views/ProductEditor.fxml", "Редактирование товара"),
    CREATE_CATEGORY("/views/CategoryEditor.fxml", "Новая категория"),
    EDIT_CATEGORY("/views/CategoryEditor.fxml", "Редактирование категории"),
    CREATE_USER("/views/UserEditor.fxml", "Новый пользователь"),
    EDIT_USER("/views/UserEditor.fxml", "Редактирование пользователя"),
    CREATE_WAREHOUSE("/views/WarehouseEditor.fxml", "Новый склад"),
    EDIT_WAREHOUSE("/views/WarehouseEditor.fxml", "Редактирование склада"),
    SET_STOCK("/views/StockEditor.fxml", "Установка остатка на складе"),
    CHANGE_ORDER_STATUS("/views/ChangeOrderStatus.fxml", "Изменение статуса");

    private final String fxmlPath;
    private final String title;

    ViewType(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    public String getFxmlPath() { return fxmlPath; }
    public String getTitle() { return title; }
}
