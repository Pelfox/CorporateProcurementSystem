package com.team.corporate.backend.controllers;

import com.team.corporate.core.dto.CategoryProductCountResponse;
import com.team.corporate.core.dto.OrderStatusCountResponse;
import com.team.corporate.core.dto.ProductStockCountResponse;
import com.team.corporate.core.dto.ProductsStatisticsResponse;
import com.team.corporate.core.services.StatisticsService;
import com.team.corporate.core.services.SystemStatistics;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class StatisticsController {
    private final StatisticsService statistics;

    public StatisticsController(StatisticsService statistics) {
        this.statistics = statistics;
    }

    @GetMapping("/summary")
    public SystemStatistics getSummary() {
        return statistics.getStatistics();
    }

    @GetMapping("/products")
    public ProductsStatisticsResponse getProductStatistics() {
        return statistics.getProductStatistics();
    }

    @GetMapping("/products/by-category")
    public List<CategoryProductCountResponse> getProductsByCategory() {
        return statistics.getProductsByCategory();
    }

    @GetMapping("/orders/by-status")
    public List<OrderStatusCountResponse> getOrdersByStatus() {
        return statistics.getOrdersByStatus();
    }

    @GetMapping("/products/by-stock")
    public List<ProductStockCountResponse> getProductsByStock() {
        return statistics.getProductsByStock();
    }
}