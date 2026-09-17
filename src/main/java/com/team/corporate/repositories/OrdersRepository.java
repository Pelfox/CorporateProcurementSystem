package com.team.corporate.repositories;

import com.team.corporate.models.Order;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdersRepository {
    @NotNull Order add(@NotNull Order order);

    @NotNull List<Order> getAll();

    Optional<Order> getById(@NotNull UUID id);

    @NotNull Order update(@NotNull Order order);

    void delete(@NotNull UUID id);
}
