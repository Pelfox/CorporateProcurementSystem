package com.team.corporate.core.services.impls;

import com.team.corporate.core.entities.OrderTag;
import com.team.corporate.core.repositories.TagsRepository;
import com.team.corporate.core.services.TagsService;
import com.team.corporate.core.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class TagsServiceImpl implements TagsService {
    private final TagsRepository tagsRepository;

    public TagsServiceImpl(@NotNull TagsRepository tagsRepository) {
        this.tagsRepository = tagsRepository;
    }

    @Override
    @NotNull
    public OrderTag attachToOrder(@NotNull UUID orderId, @NotNull String tagName, @Nullable String color) {
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        EntityValidation.requireText(tagName, "Название тега");
        return tagsRepository.attachToOrder(orderId, tagName, color);
    }

    @Override
    public boolean detachFromOrder(@NotNull UUID orderId, @NotNull String tagName) {
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        EntityValidation.requireText(tagName, "Название тега");
        return tagsRepository.detachFromOrder(orderId, tagName);
    }

    @Override
    @NotNull
    public List<OrderTag> getByOrder(@NotNull UUID orderId) {
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        return tagsRepository.getByOrderId(orderId);
    }

    @Override
    @NotNull
    public List<OrderTag> getAll() { return tagsRepository.getAll(); }

    @Override
    @NotNull
    public List<UUID> getOrderIdsByTag(@NotNull String tagName) {
        EntityValidation.requireText(tagName, "Название тега");
        return tagsRepository.getOrderIdsByTag(tagName);
    }
}