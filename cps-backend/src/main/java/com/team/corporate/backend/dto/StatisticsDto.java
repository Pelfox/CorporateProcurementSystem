package com.team.corporate.backend.dto;

import com.team.corporate.core.services.SystemStatistics;

import java.math.BigDecimal;

public record StatisticsDto(
        long totalUsers,
        long totalProducts,
        long totalCategories,
        long totalWarehouses,
        long totalOrders,
        long activeOrders,
        long createdOrders,
        long approvedOrders,
        long deliveredOrders,
        long cancelledOrders,
        long totalStockQuantity,
        BigDecimal totalOrderValue
) {
    public static StatisticsDto from(SystemStatistics stats) {
        return new StatisticsDto(
                stats.totalUsers(),
                stats.totalProducts(),
                stats.totalCategories(),
                stats.totalWarehouses(),
                stats.totalOrders(),
                stats.activeOrders(),
                stats.createdOrders(),
                stats.approvedOrders(),
                stats.deliveredOrders(),
                stats.cancelledOrders(),
                stats.totalStockQuantity(),
                stats.totalOrderValue()
        );
    }

    public record WarehouseStock(String warehouseName, long quantity) {
    }
}
