package com.team.corporate.services.impls;

import com.team.corporate.entities.OrderItem;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.exceptions.ProductNotFoundException;
import com.team.corporate.repositories.InventoriesRepository;
import com.team.corporate.repositories.OrderItemsRepository;
import com.team.corporate.repositories.ProductsRepository;
import com.team.corporate.repositories.Transactions;
import com.team.corporate.services.StockService;

import java.util.*;

public final class OrderStock implements StockService {
    private final ProductsRepository products;
    private final InventoriesRepository inventories;
    private final OrderItemsRepository items;
    private final Transactions transactions;

    public OrderStock(ProductsRepository products, InventoriesRepository inventories,
                      OrderItemsRepository items, Transactions transactions) {
        this.products = products;
        this.inventories = inventories;
        this.items = items;
        this.transactions = transactions;
    }

    public static boolean reservesStock(OrderStatus status) {
        return status == OrderStatus.CREATED || status == OrderStatus.APPROVED;
    }

    public void lockProducts(Collection<UUID> ids) {
        for (UUID id : ids.stream().distinct().sorted().toList()) {
            products.getByIdForUpdate(id).orElseThrow(() -> new ProductNotFoundException("Товар не найден."));
        }
    }

    @Override
    public Map<UUID, Long> getAvailableQuantities(UUID editingOrderId) {
        return transactions.execute(() -> {
            Map<UUID, Long> available = new HashMap<>();
            for (var inventory : inventories.getAll()) {
                available.merge(inventory.getProduct().getId(), (long) inventory.getQuantity(), Long::sum);
            }
            for (OrderItem item : items.getAll()) {
                if (reservesStock(item.getOrder().getStatus()) && !item.getOrder().getId().equals(editingOrderId)) {
                    available.merge(item.getProduct().getId(), -(long) item.getQuantity(), Long::sum);
                }
            }
            available.replaceAll((id, quantity) -> Math.max(0, quantity));
            return Map.copyOf(available);
        });
    }

    public Map<UUID, Long> quantities(UUID orderId) {
        Map<UUID, Long> result = new HashMap<>();
        for (OrderItem item : items.getByOrderId(orderId)) {
            result.merge(item.getProduct().getId(), (long) item.getQuantity(), Long::sum);
        }
        return result;
    }

    /**
     * Must be called inside the transaction that writes the order.
     */
    public void requireAvailable(Map<UUID, Long> requested, UUID editingOrderId) {
        if (requested.values().stream().anyMatch(quantity -> quantity < 1
                || quantity > com.team.corporate.utils.EntityValidation.ORDER_LINE_MAX_QUANTITY)) {
            throw new IllegalArgumentException("Количество одного товара в заказе: от 1 до "
                    + com.team.corporate.utils.EntityValidation.ORDER_LINE_MAX_QUANTITY + ".");
        }
        lockProducts(requested.keySet());
        Map<UUID, Long> available = getAvailableQuantities(editingOrderId);
        requested.forEach((id, quantity) -> {
            long remaining = available.getOrDefault(id, 0L);
            if (quantity > remaining) {
                String name = products.getById(id).orElseThrow().getName();
                throw new IllegalArgumentException("Недостаточно товара «" + name + "» на складах. Доступно: "
                        + remaining + ", запрошено: " + quantity + ".");
            }
        });
    }

    public void deliver(UUID orderId) {
        Map<UUID, Long> requested = quantities(orderId);
        requireAvailable(requested, orderId);
        for (var entry : requested.entrySet()) {
            long remaining = entry.getValue();
            var stock = inventories.getByProductId(entry.getKey()).stream()
                    .sorted(Comparator.comparing(inventory -> inventory.getId())).toList();
            for (var inventory : stock) {
                int taken = (int) Math.min(remaining, inventory.getQuantity());
                inventory.setQuantity(inventory.getQuantity() - taken);
                inventories.update(inventory);
                remaining -= taken;
                if (remaining == 0) break;
            }
        }
    }

    public void requireInventoryFloor(UUID productId, long newTotal) {
        long reserved = items.getByProductId(productId).stream()
                .filter(item -> reservesStock(item.getOrder().getStatus())).mapToLong(OrderItem::getQuantity).sum();
        if (newTotal < reserved) {
            throw new IllegalArgumentException("Нельзя уменьшить остаток ниже количества в активных заказах: " + reserved + ".");
        }
    }
}
