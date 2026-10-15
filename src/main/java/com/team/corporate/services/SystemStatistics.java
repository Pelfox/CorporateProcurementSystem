package com.team.corporate.services;

import java.math.BigDecimal;

public record SystemStatistics(
        long totalUsers,
        long totalProducts,
        long totalCategories,
        long totalWarehouses,
        long totalOrders,
        long createdOrders,
        long approvedOrders,
        long deliveredOrders,
        long cancelledOrders,
        long totalStockQuantity,
        BigDecimal totalOrderValue
) {
    public long activeOrders() {
        return createdOrders + approvedOrders;
    }
}
