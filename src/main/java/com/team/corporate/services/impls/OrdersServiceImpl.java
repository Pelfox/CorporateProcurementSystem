package com.team.corporate.services.impls;

import com.team.corporate.entities.*;
import com.team.corporate.exceptions.OrderNotFoundException;
import com.team.corporate.exceptions.UserNotFoundException;
import com.team.corporate.repositories.*;
import com.team.corporate.services.OrdersService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrdersServiceImpl implements OrdersService {
    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;

    private final ProductsRepository productsRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final AuditLogsRepository auditLogsRepository;
    private final Transactions transactions;

    public OrdersServiceImpl(@NotNull OrdersRepository ordersRepository,
                             @NotNull UsersRepository usersRepository,
                             @NotNull ProductsRepository productsRepository,
                             @NotNull OrderItemsRepository orderItemsRepository,
                             @NotNull AuditLogsRepository auditLogsRepository,
                             @NotNull Transactions transactions) {
        this.ordersRepository = ordersRepository;
        this.usersRepository = usersRepository;
        this.productsRepository = productsRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.auditLogsRepository = auditLogsRepository;
        this.transactions = transactions;
    }

    @Override
    @NotNull
    public Order createOrder(@NotNull UUID userId, @NotNull OrderStatus status, @Nullable String notes) {
        User user = usersRepository.getById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с указанным ID не найден"));
        Order order = new Order(user, status, notes);
        return ordersRepository.add(order);
    }

    @Override
    public void deleteOrder(@NotNull UUID id) {
        ordersRepository.delete(id);
    }

    @Override
    @NotNull
    public Order updateOrder(@NotNull UUID id, @Nullable OrderStatus status, @Nullable String notes) {
        Order order = ordersRepository.getById(id)
                .orElseThrow(() -> new OrderNotFoundException("Заказ с указанным ID не найден"));
        if (status != null) {
            order.setStatus(status);
        }
        if (notes != null) {
            order.setNotes(notes);
        }
        return ordersRepository.update(order);
    }

    @Override
    @NotNull
    public List<Order> getAllByUser(@NotNull UUID userId) {
        usersRepository.getById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с указанным ID не найден"));
        return ordersRepository.getAll()
                .stream()
                .filter(order -> order.getUser().getId().equals(userId))
                .toList();
    }

    @Override
    @NotNull
    public Optional<Order> getOrder(@NotNull UUID id) {
        return ordersRepository.getById(id);
    }

    @Override
    @NotNull
    public List<Order> getAllByStatus(@NotNull OrderStatus status) {
        return ordersRepository.getAll()
                .stream()
                .filter(order -> order.getStatus().equals(status))
                .toList();
    }

    @Override
    @NotNull
    public List<Order> getAll() {
        return ordersRepository.getAll();
    }

    @NotNull
    private Order editable(@NotNull UUID userId, @NotNull UUID orderId) {
        Order order = ordersRepository.getByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден."));
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Заказ не найден.");
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalArgumentException("Можно редактировать только созданный заказ.");
        }
        return order;
    }

    @Override
    @NotNull
    public UUID createOrder(@NotNull UUID userId, @NotNull List<Line> lines, @Nullable String notes) {
        List<Line> snapshot = List.copyOf(lines);
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("Добавьте хотя бы один товар.");
        }
        return transactions.execute(() -> {
            Order order = createOrder(userId, OrderStatus.CREATED, notes);
            for (Line line : snapshot) {
                addItem(order, line);
            }
            return order.getId();
        });
    }

    private void addItem(@NotNull Order order, @NotNull Line line) {
        Product product = productsRepository.getById(line.productId())
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден."));
        orderItemsRepository.add(new OrderItem(order, product, line.quantity(), product.getPrice()));
    }

    @Override
    public void updateNotes(@NotNull UUID userId, @NotNull UUID orderId, @Nullable String notes) {
        transactions.execute(() -> {
            Order order = editable(userId, orderId);
            order.setNotes(notes);
            ordersRepository.update(order);
        });
    }

    @Override
    public void addItem(@NotNull UUID userId, @NotNull UUID orderId, @NotNull Line line) {
        transactions.execute(() -> addItem(editable(userId, orderId), line));
    }

    @Override
    public void updateQuantity(@NotNull UUID userId, @NotNull UUID orderId, @NotNull UUID itemId, int quantity) {
        transactions.execute(() -> {
            editable(userId, orderId);
            OrderItem item = orderItemsRepository.getById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Позиция не найдена."));
            if (!item.getOrder().getId().equals(orderId)) {
                throw new IllegalArgumentException("Позиция не принадлежит заказу.");
            }
            if (quantity == 0) {
                if (orderItemsRepository.getByOrderId(orderId).size() <= 1) {
                    throw new IllegalArgumentException("В заказе должна остаться хотя бы одна позиция.");
                }
                orderItemsRepository.delete(itemId);
            } else {
                item.setQuantity(quantity);
                orderItemsRepository.update(item);
            }
        });
    }

    @Override
    public void changeStatus(@NotNull UUID managerId, @NotNull UUID orderId, @NotNull OrderStatus status) {
        transactions.execute(() -> {
            User manager = usersRepository.getById(managerId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));
            if (manager.getUserRole() != UserRole.MANAGER) {
                throw new IllegalArgumentException("Доступ только для менеджера.");
            }
            Order order = ordersRepository.getByIdForUpdate(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден."));
            if (order.getStatus() == status) {
                return;
            }
            auditLogsRepository.add(new AuditLog(order, manager, order.getStatus(), status));
            order.setStatus(status);
            ordersRepository.update(order);
        });
    }
}
