package com.team.corporate.backend.controllers;

import com.team.corporate.core.dto.CategoryProductCountResponse;
import com.team.corporate.core.dto.OrderStatusCountResponse;
import com.team.corporate.core.dto.ProductStockCountResponse;
import com.team.corporate.core.dto.ProductsStatisticsResponse;
import com.team.corporate.core.services.StatisticsService;
import com.team.corporate.core.services.SystemStatistics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Статистика", description = "Сводные показатели закупок, товаров и складских остатков. Только MANAGER и ADMIN после смены временного пароля.")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class StatisticsController {
    private final StatisticsService statistics;

    public StatisticsController(StatisticsService statistics) {
        this.statistics = statistics;
    }

    @GetMapping("/summary")
    @Operation(summary = "Получить сводную статистику системы", description = "Возвращает количество пользователей, товаров, категорий, складов и заказов, "
            + "число заказов по статусам, общий складской остаток и сумму quantity × purchasePrice по позициям заказов.")
    public SystemStatistics getSummary() {
        return statistics.getStatistics();
    }

    @GetMapping("/products")
    @Operation(summary = "Получить сводную статистику товаров", description = "total учитывает все товары; active и archived разделяют их по признаку удаления. "
            + "withTags — активные товары с тегами; averagePrice — средняя цена активных товаров с округлением до двух знаков или null при их отсутствии.")
    public ProductsStatisticsResponse getProductStatistics() {
        return statistics.getProductStatistics();
    }

    @GetMapping("/products/by-category")
    @Operation(summary = "Получить количество товаров по категориям", description = "Учитывает активные товары. Для товаров без категории возвращает группу с categoryId=null и названием «Без категории».")
    public List<CategoryProductCountResponse> getProductsByCategory() {
        return statistics.getProductsByCategory();
    }

    @GetMapping("/orders/by-status")
    @Operation(summary = "Получить количество заказов по статусам", description = "Учитывает активные заказы. Возвращает все статусы, включая группы с нулевым количеством.")
    public List<OrderStatusCountResponse> getOrdersByStatus() {
        return statistics.getOrdersByStatus();
    }

    @GetMapping("/products/by-stock")
    @Operation(summary = "Получить количество товаров по уровню остатка", description = "Суммирует остатки активных товаров на активных складах. OUT_OF_STOCK — 0, LOW_STOCK — от 1 до 5, IN_STOCK — больше 5. "
            + "Возвращает все уровни, включая группы с нулевым количеством.")
    public List<ProductStockCountResponse> getProductsByStock() {
        return statistics.getProductsByStock();
    }
}
