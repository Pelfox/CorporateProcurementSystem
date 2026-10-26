package com.team.corporate.desktop.controllers.requests;

import com.team.corporate.core.entities.Order;
import com.team.corporate.core.entities.User;

import java.util.UUID;

public record OrderStatusChangeRequest(UUID orderId, UUID managerId) {
    public static OrderStatusChangeRequest of(Order order, User manager) {
        return new OrderStatusChangeRequest(order.getId(), manager.getId());
    }
}
