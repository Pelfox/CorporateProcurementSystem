package com.team.corporate.core.services;

import com.team.corporate.core.entities.OrderTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface TagsService {
    @NotNull OrderTag attachToOrder(@NotNull UUID orderId, @NotNull String tagName, @Nullable String color);
    boolean detachFromOrder(@NotNull UUID orderId, @NotNull String tagName);
    @NotNull List<OrderTag> getByOrder(@NotNull UUID orderId);
    @NotNull List<OrderTag> getAll();
    @NotNull List<UUID> getOrderIdsByTag(@NotNull String tagName);
}