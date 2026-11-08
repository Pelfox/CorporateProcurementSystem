package com.team.corporate.core.repositories;

import com.team.corporate.core.entities.OrderTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagsRepository {
    @NotNull OrderTag add(@NotNull OrderTag tag);
    @NotNull Optional<OrderTag> getById(@NotNull UUID id);
    @NotNull Optional<OrderTag> getByName(@NotNull String name);
    @NotNull List<OrderTag> getAll();
    void delete(@NotNull UUID id);

    @NotNull OrderTag attachToOrder(@NotNull UUID orderId, @NotNull String tagName, @Nullable String color);
    boolean detachFromOrder(@NotNull UUID orderId, @NotNull String tagName);
    @NotNull List<OrderTag> getByOrderId(@NotNull UUID orderId);
    @NotNull List<UUID> getOrderIdsByTag(@NotNull String tagName);
}