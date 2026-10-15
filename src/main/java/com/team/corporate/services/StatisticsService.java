package com.team.corporate.services;

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
