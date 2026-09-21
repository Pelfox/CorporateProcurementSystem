package com.team.corporate.services;

import com.team.corporate.entities.Order;
import com.team.corporate.entities.OrderStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdersService {
    @NotNull
    Order createOrder(@NotNull UUID userId, @NotNull OrderStatus status, @Nullable String notes);

    void deleteOrder(@NotNull UUID id);

    @NotNull
    Order updateOrder(@NotNull UUID id, @Nullable OrderStatus status, @Nullable String notes);

    @NotNull
    List<Order> getAllByUser(@NotNull UUID userId);

    @NotNull
    Optional<Order> getOrder(@NotNull UUID id);

    @NotNull
    List<Order> getAllByStatus(@NotNull OrderStatus status);

    @NotNull
    List<Order> getAll();

    @NotNull
    UUID createOrder(@NotNull UUID userId, @NotNull List<Line> lines, @Nullable String notes);

    void updateNotes(@NotNull UUID userId, @NotNull UUID orderId, @Nullable String notes);

    void addItem(@NotNull UUID userId, @NotNull UUID orderId, @NotNull Line line);

    void updateQuantity(@NotNull UUID userId, @NotNull UUID orderId, @NotNull UUID itemId, int quantity);

    void changeStatus(@NotNull UUID managerId, @NotNull UUID orderId, @NotNull OrderStatus status);

    record Line(UUID productId, int quantity) {
        public Line {
            if (productId == null || quantity < 1) {
                throw new IllegalArgumentException("Некорректная позиция заказа.");
            }
        }
    }
}
