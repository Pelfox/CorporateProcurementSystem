package com.team.corporate.domain.repository;

import com.team.corporate.domain.models.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order add(Order order);
    List<Order> getAll();
    Optional<Order> getById(UUID id);
    Order update(Order order);
    void delete(UUID id);
}
