package com.team.corporate.services.impls;

import com.team.corporate.entities.Order;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.entities.User;
import com.team.corporate.exceptions.OrderNotFoundException;
import com.team.corporate.exceptions.UserNotFoundException;
import com.team.corporate.repositories.OrdersRepository;
import com.team.corporate.repositories.UsersRepository;
import com.team.corporate.services.OrdersService;
import jakarta.persistence.EntityNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrdersServiceImpl implements OrdersService {
    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;

    public OrdersServiceImpl(OrdersRepository ordersRepository, UsersRepository usersRepository) {
        this.ordersRepository = ordersRepository;
        this.usersRepository = usersRepository;
    }

    @Override
    public @NotNull Order createOrder(@NotNull UUID userId, @NotNull OrderStatus status, @Nullable String notes) {
        User user = usersRepository.getById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с указанным id не найден"));

        Order order = new Order(user, status, notes);
        return ordersRepository.add(order);
    }

    @Override
    public void deleteOrder(@NotNull UUID id) {
        ordersRepository.delete(id);
    }

    @Override
    public @NotNull Order updateOrder(@NotNull UUID id, @Nullable OrderStatus status, @Nullable String notes) {
        Order order = ordersRepository.getById(id)
                .orElseThrow(() -> new OrderNotFoundException("Заказ с указанным id не найден"));

        if (status != null) {
            order.setStatus(status);
        }
        if (notes != null) {
            order.setNotes(notes);
        }
        return ordersRepository.update(order);
    }

    @Override
    public @NotNull List<Order> getAllByUser(@NotNull UUID userId) {
        usersRepository.getById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с указанным id не найден"));

        return ordersRepository.getAll() // Требуется реализовать отдельный метод в OrdersRepository.
                .stream().filter(order -> order.getUser().getId().equals(userId))
                .toList();
    }

    @Override
    public @NotNull Optional<Order> getOrder(@NotNull UUID id) {
        return ordersRepository.getById(id);
    }

    @Override
    public @NotNull List<Order> getAllByStatus(@NotNull OrderStatus status) {
        return ordersRepository.getAll() // Требуется реализовать отдельный метод в OrdersRepository.
                .stream().filter(order -> order.getStatus().equals(status))
                .toList();
    }

    @Override
    public @NotNull List<Order> getAll() {
        return ordersRepository.getAll();
    }
}
