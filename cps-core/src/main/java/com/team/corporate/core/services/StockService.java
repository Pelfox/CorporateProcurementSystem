package com.team.corporate.core.services;

import java.util.Map;
import java.util.UUID;

public interface StockService {
    Map<UUID, Long> getAvailableQuantities(UUID editingOrderId);
}
