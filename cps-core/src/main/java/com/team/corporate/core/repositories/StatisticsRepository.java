package com.team.corporate.core.repositories;

import com.team.corporate.core.dto.CategoryProductCountResponse;
import com.team.corporate.core.dto.OrderStatusCountResponse;
import com.team.corporate.core.dto.ProductStockCountResponse;
import com.team.corporate.core.dto.ProductsStatisticsResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface StatisticsRepository {

    @NotNull
    ProductsStatisticsResponse getProductStatistics();

    @NotNull
    List<CategoryProductCountResponse> getProductsByCategory();

    @NotNull
    List<OrderStatusCountResponse> getOrdersByStatus();

    @NotNull
    List<ProductStockCountResponse> getProductsByStock(int lowStockThreshold);
}
