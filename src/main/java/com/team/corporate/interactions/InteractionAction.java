package com.team.corporate.interactions;

import org.jetbrains.annotations.NotNull;

public enum InteractionAction {
    CREATE_NEW_ORDER("Оформить новый заказ"),
    ORDERS_HISTORY("Просмотр истории заказов"),
    CANCEL_ORDER("Отмена заказа"),
    EDIT_ORDER("Редактирование заказа"),
    WAREHOUSE_EDIT("Редактирование склада");

    private final String visibleName;
    InteractionAction(@NotNull String visibleName) {
        this.visibleName = visibleName;
    }

    @NotNull
    public String getVisibleName() {
        return this.visibleName;
    }
}
