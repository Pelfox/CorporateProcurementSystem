package com.team.corporate.services;

import com.team.corporate.entities.Order;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdersService {

    @NotNull Order createOrder(@NotNull UUID userId, @NotNull OrderStatus status, @Nullable String notes);

    void deleteOrder(@NotNull UUID id);

    @NotNull Order updateOrder(@NotNull UUID id, @Nullable OrderStatus status, @Nullable String notes);

    @NotNull List<Order> getAllByUser(@NotNull UUID userId);

    @NotNull Optional<Order> getOrder(@NotNull UUID id);

    @NotNull List<Order> getAllByStatus(@NotNull OrderStatus status);

    @NotNull List<Order> getAll();

}
