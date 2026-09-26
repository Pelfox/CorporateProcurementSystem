package com.team.corporate.services;

import org.jetbrains.annotations.NotNull;

public interface StatisticsService {
    @NotNull
    SystemStatistics getStatistics();
}
