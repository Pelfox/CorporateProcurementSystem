package com.team.corporate.repositories;

import com.team.corporate.entities.OrderItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemsRepository {
    @NotNull
    OrderItem add(@NotNull OrderItem orderItem);

    @NotNull
    List<OrderItem> addAll(@NotNull List<OrderItem> orderItems);

    @NotNull
    List<OrderItem> getAll();

    @NotNull
    Optional<OrderItem> getById(@NotNull UUID id);

    @NotNull
    List<OrderItem> getByOrderId(@NotNull UUID id);

    @NotNull
    List<OrderItem> getByProductId(@NotNull UUID id);

    @NotNull
    OrderItem update(@NotNull OrderItem orderItem);

    void delete(@NotNull UUID id);
}
