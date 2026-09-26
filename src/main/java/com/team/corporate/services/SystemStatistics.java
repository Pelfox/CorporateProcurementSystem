package com.team.corporate.services;

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
        long totalStockQuantity
) {
    public long activeOrders() {
        return createdOrders + approvedOrders;
    }
}
