package com.team.corporate.core.dto;

public record ProductStockCountResponse(
        StockLevel level,
        long count
) {
}
