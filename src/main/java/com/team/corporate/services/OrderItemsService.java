package com.team.corporate.services;

import com.team.corporate.entities.OrderItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemsService {
    @NotNull
    OrderItem createOrderItemForOrder(@NotNull UUID orderId, @NotNull UUID productId, int quantity, @NotNull BigDecimal purchasePrice);

    void deleteOrderItem(@NotNull UUID id);

    @NotNull
    OrderItem updateOrderItem(@NotNull UUID id, @Nullable Integer quantity, @Nullable BigDecimal purchasePrice);

    @NotNull
    List<OrderItem> getAllByOrder(@NotNull UUID orderId);

    @NotNull
    Optional<OrderItem> getOrderItem(@NotNull UUID id);

    @NotNull
    List<OrderItem> getAllByProduct(@NotNull UUID productId);

    @NotNull
    List<OrderItem> getAll();
}
