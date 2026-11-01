package com.team.corporate.desktop.controllers.dialog;

public enum ViewType {
    CREATE_ORDER("/fxml/orders/OrderCreateDialog.fxml", "Новый заказ"),
    EDIT_ORDER("/fxml/orders/OrderCreateDialog.fxml", "Редактирование заказа"),
    CREATE_PRODUCT("/fxml/products/ProductDialog.fxml", "Новый товар"),
    EDIT_PRODUCT("/fxml/products/ProductDialog.fxml", "Редактирование товара"),
    CREATE_CATEGORY("/fxml/categories/CategoryDialog.fxml", "Новая категория"),
    EDIT_CATEGORY("/fxml/categories/CategoryDialog.fxml", "Редактирование категории"),
    CREATE_USER("/fxml/users/UserDialog.fxml", "Новый пользователь"),
    EDIT_USER("/fxml/users/UserDialog.fxml", "Редактирование пользователя"),
    CREATE_WAREHOUSE("/fxml/warehouses/WarehouseDialog.fxml", "Новый склад"),
    EDIT_WAREHOUSE("/fxml/warehouses/WarehouseDialog.fxml", "Редактирование склада"),
    SET_STOCK("/fxml/warehouses/InventoryDialog.fxml", "Установка остатка на складе"),
    CHANGE_ORDER_STATUS("/fxml/orders/OrderStatusDialog.fxml", "Изменение статуса"),
    CONFIRM_DELETE("/fxml/common/ConfirmDeleteDialog.fxml", "Подтверждение удаления"),
    ERROR("/fxml/common/ErrorDialog.fxml", "Ошибка"),
    SUCCESS("/fxml/common/SuccessDialog.fxml", "Успешно");

    private final String fxmlPath;
    private final String title;

    ViewType(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public String getTitle() {
        return title;
    }
}
