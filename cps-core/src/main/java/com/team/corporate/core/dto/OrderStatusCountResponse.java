package com.team.corporate.core.dto;

import com.team.corporate.core.entities.OrderStatus;

public record OrderStatusCountResponse(
        OrderStatus status,
        long count
) {
}
