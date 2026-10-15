package com.team.corporate.services;

import java.util.Map;
import java.util.UUID;

public interface StockService {
    /**
     * Available stock across warehouses, excluding reservations of the order being edited.
     */
    Map<UUID, Long> getAvailableQuantities(UUID editingOrderId);
}
