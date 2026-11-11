package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.StatisticsDto;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.core.services.StatisticsService;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsHandler {
    private final StatisticsService statistics;
    private final ApiExecutor api;

    public StatisticsHandler(@NotNull StatisticsService statistics, @NotNull ApiExecutor api) {
        this.statistics = statistics;
        this.api = api;
    }

    @GetMapping
    public StatisticsDto summary() {
        return api.execute(() -> StatisticsDto.from(statistics.getStatistics()));
    }

    @GetMapping("/warehouses")
    public List<StatisticsDto.WarehouseStock> warehouses() {
        return api.execute(() -> statistics.getWarehouseStocks().stream()
                .map(stock -> new StatisticsDto.WarehouseStock(stock.warehouseName(), stock.quantity())).toList());
    }
}
