package com.team.corporate.controllers.requests;

import com.team.corporate.entities.Order;

import java.util.UUID;

public record OrderEditorRequest(UUID userId, UUID orderId) {
    public static OrderEditorRequest forCreate(UUID userId) {
        return new OrderEditorRequest(userId, null);
    }

    public static OrderEditorRequest forEdit(UUID userId, Order order) {
        return new OrderEditorRequest(userId, order.getId());
    }

    public boolean isEditing() {
        return orderId != null;
    }
}
