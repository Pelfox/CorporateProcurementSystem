package com.team.corporate.domain.repository;

import com.team.corporate.domain.models.OrderItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository {
    OrderItem add(OrderItem orderItem);
    List<OrderItem> addAll(List<OrderItem> orderItems);
    List<OrderItem> getAll();
    Optional<OrderItem> getById(UUID id);
    List<OrderItem> getByOrderId(UUID id);
    List<OrderItem> getByProductId(UUID id);
    OrderItem update(OrderItem orderItem);
    void delete(UUID id);
}
