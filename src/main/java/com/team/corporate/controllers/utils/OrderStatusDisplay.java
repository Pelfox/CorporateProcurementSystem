package com.team.corporate.controllers.utils;

import com.team.corporate.entities.OrderStatus;

public final class OrderStatusDisplay {
    private OrderStatusDisplay() {
    }

    public static String name(OrderStatus status) {
        return switch (status) {
            case CREATED -> "Создан";
            case APPROVED -> "Одобрен";
            case DELIVERED -> "Доставлен";
            case CANCELLED -> "Отменён";
        };
    }

    public static String badgeStyle(OrderStatus status) {
        return switch (status) {
            case CREATED -> "status-created";
            case APPROVED -> "status-approved";
            case DELIVERED -> "status-delivered";
            case CANCELLED -> "status-cancelled";
        };
    }
}
