package com.team.corporate.core.services;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface StatisticsService {
    @NotNull
    SystemStatistics getStatistics();

    @NotNull
    List<WarehouseStock> getWarehouseStocks();

    record WarehouseStock(String warehouseName, long quantity) {
    }
}
