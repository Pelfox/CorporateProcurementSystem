package com.team.corporate.core.dto;

import java.math.BigDecimal;

public record ProductsStatisticsResponse(
        long total,
        long active,
        long archived,
        long withTags,
        BigDecimal averagePrice
) {
}