package com.team.corporate.core.services.impls;

import com.team.corporate.core.entities.*;
import com.team.corporate.core.exceptions.OrderNotFoundException;
import com.team.corporate.core.exceptions.UserNotFoundException;
import com.team.corporate.core.repositories.*;
import com.team.corporate.core.services.OrdersService;
import com.team.corporate.core.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OrdersServiceImpl implements OrdersService {
    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;

    private final ProductsRepository productsRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final AuditLogsRepository auditLogsRepository;
    private final Transactions transactions;
    private final OrderStock stock;

    public OrdersServiceImpl(@NotNull OrdersRepository ordersRepository,
                             @NotNull UsersRepository usersRepository,
                             @NotNull ProductsRepository productsRepository,
                             @NotNull OrderItemsRepository orderItemsRepository,
                             @NotNull AuditLogsRepository auditLogsRepository,
                             @NotNull Transactions transactions,
                             @NotNull OrderStock stock) {
        this.ordersRepository = ordersRepository;
        this.usersRepository = usersRepository;
        this.productsRepository = productsRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.auditLogsRepository = auditLogsRepository;
        this.transactions = transactions;
        this.stock = stock;
    }

    private static Map<UUID, Long> quantities(List<Line> lines) {
        Map<UUID, Long> quantities = new HashMap<>();
        for (Line line : lines) quantities.merge(line.productId(), (long) line.quantity(), Long::sum);
        return quantities;
    }

    private static List<Line> normalized(List<Line> lines) {
        Map<UUID, Long> quantities = quantities(lines);
        List<Line> result = new ArrayList<>();
        quantities.forEach((product, count) -> {
            if (count > EntityValidation.ORDER_LINE_MAX_QUANTITY)
                throw new IllegalArgumentException("Слишком большое количество товара в заказе.");
            result.add(new Line(product, count.intValue()));
        });
        return result;
    }

    @Override
    @NotNull
    public Order createOrder(@NotNull UUID userId, @NotNull OrderStatus status, @Nullable String notes) {
        EntityValidation.requireNonNull(userId, "Идентификатор пользователя");
        EntityValidation.requireNonNull(status, "Статус");
        User user = usersRepository.getById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с указанным идентификатором не найден"));
        Order order = new Order(user, status, notes);
        return ordersRepository.add(order);
    }

    @Override
    public void deleteOrder(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        transactions.execute(() -> {
            if (ordersRepository.getByIdForUpdate(id).isEmpty()) return;
            // Сначала удаляем дочерние сущности, чтобы не нарушить Foreign Key
            for (OrderItem item : orderItemsRepository.getByOrderId(id)) {
                orderItemsRepository.delete(item.getId());
            }
            for (AuditLog log : auditLogsRepository.getByOrderId(id)) {
                auditLogsRepository.delete(log.getId());
            }
            // И теперь заказ
            ordersRepository.delete(id);
        });
    }

    @Override
    @NotNull
    public Order updateOrder(@NotNull UUID id, @Nullable OrderStatus status, @Nullable String notes) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return transactions.execute(() -> {
            Order order = ordersRepository.getByIdForUpdate(id)
                    .orElseThrow(() -> new OrderNotFoundException("Заказ с указанным идентификатором не найден"));
            EntityValidation.optionalText(notes, "Примечание");
            if (status != null && status != order.getStatus()) transitionStock(order, status);
            if (status != null) order.setStatus(status);
            if (notes != null) order.setNotes(notes);
            return ordersRepository.update(order);
        });
    }

    @Override
    @NotNull
    public List<Order> getAllByUser(@NotNull UUID userId) {
        EntityValidation.requireNonNull(userId, "Идентификатор пользователя");
        usersRepository.getById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с указанным идентификатором не найден"));
        return ordersRepository.getAll()
                .stream()
                .filter(order -> order.getUser().getId().equals(userId))
                .toList();
    }

    @Override
    @NotNull
    public Optional<Order> getOrder(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return ordersRepository.getById(id);
    }

    @Override
    @NotNull
    public List<Order> getAllByStatus(@NotNull OrderStatus status) {
        EntityValidation.requireNonNull(status, "Статус");
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
        EntityValidation.requireNonNull(userId, "Идентификатор пользователя");
        EntityValidation.requireNonNull(lines, "Позиции заказа");
        for (Line line : lines) {
            EntityValidation.requireNonNull(line, "Позиция заказа");
        }
        List<Line> snapshot = normalized(lines);
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("Добавьте хотя бы один товар.");
        }
        EntityValidation.optionalText(notes, "Примечание");
        return transactions.execute(() -> {
            usersRepository.getById(userId).orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
            stock.requireAvailable(quantities(snapshot), null);
            Order order = createOrder(userId, OrderStatus.CREATED, notes);
            for (Line line : snapshot) {
                addItem(order, line);
            }
            return order.getId();
        });
    }

    @Override
    public void saveOrder(@NotNull UUID userId,
                          @NotNull UUID orderId,
                          @NotNull List<Line> lines,
                          @Nullable String notes) {
        EntityValidation.requireNonNull(userId, "Идентификатор пользователя");
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        EntityValidation.requireNonNull(lines, "Позиции заказа");
        for (Line line : lines) {
            EntityValidation.requireNonNull(line, "Позиция заказа");
        }
        List<Line> snapshot = normalized(lines);
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("В заказе должна остаться хотя бы одна позиция.");
        }
        EntityValidation.optionalText(notes, "Примечание");

        transactions.execute(() -> {
            Order order = editable(userId, orderId);
            stock.requireAvailable(quantities(snapshot), orderId);
            Map<UUID, OrderItem> current = new LinkedHashMap<>();
            for (OrderItem item : orderItemsRepository.getByOrderId(orderId)) {
                if (current.putIfAbsent(item.getProduct().getId(), item) != null)
                    orderItemsRepository.delete(item.getId());
            }
            for (Line line : snapshot) {
                OrderItem existing = current.remove(line.productId());
                if (existing == null) {
                    addItem(order, line);
                } else if (existing.getQuantity() != line.quantity()) {
                    existing.setQuantity(line.quantity());
                    orderItemsRepository.update(existing);
                }
            }
            for (OrderItem removed : current.values()) {
                orderItemsRepository.delete(removed.getId());
            }
            order.setNotes(notes);
            ordersRepository.update(order);
        });
    }

    private void addItem(@NotNull Order order, @NotNull Line line) {
        Product product = productsRepository.getById(line.productId())
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден."));
        orderItemsRepository.add(new OrderItem(order, product, line.quantity(), product.getPrice()));
    }

    @Override
    public void updateNotes(@NotNull UUID userId, @NotNull UUID orderId, @Nullable String notes) {
        EntityValidation.requireNonNull(userId, "Идентификатор пользователя");
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        transactions.execute(() -> {
            Order order = editable(userId, orderId);
            order.setNotes(notes);
            ordersRepository.update(order);
        });
    }

    @Override
    public void addItem(@NotNull UUID userId, @NotNull UUID orderId, @NotNull Line line) {
        EntityValidation.requireNonNull(userId, "Идентификатор пользователя");
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        EntityValidation.requireNonNull(line, "Позиция заказа");
        transactions.execute(() -> {
            Order order = editable(userId, orderId);
            Map<UUID, Long> requested = stock.quantities(orderId);
            requested.merge(line.productId(), (long) line.quantity(), Long::sum);
            stock.requireAvailable(requested, orderId);
            addItem(order, line);
        });
    }

    @Override
    public void updateQuantity(@NotNull UUID userId, @NotNull UUID orderId, @NotNull UUID itemId, int quantity) {
        EntityValidation.requireNonNull(userId, "Идентификатор пользователя");
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        EntityValidation.requireNonNull(itemId, "Идентификатор позиции");
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
                EntityValidation.requireOrderQuantity(quantity);
                Map<UUID, Long> requested = stock.quantities(orderId);
                requested.merge(item.getProduct().getId(), (long) quantity - item.getQuantity(), Long::sum);
                stock.requireAvailable(requested, orderId);
                item.setQuantity(quantity);
                orderItemsRepository.update(item);
            }
        });
    }

    @Override
    public void changeStatus(@NotNull UUID managerId, @NotNull UUID orderId, @NotNull OrderStatus status) {
        EntityValidation.requireNonNull(managerId, "Идентификатор менеджера");
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        EntityValidation.requireNonNull(status, "Статус");
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
            transitionStock(order, status);
            auditLogsRepository.add(new AuditLog(order, manager, order.getStatus(), status));
            order.setStatus(status);
            ordersRepository.update(order);
        });
    }

    private void transitionStock(Order order, OrderStatus status) {
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Доставленный заказ нельзя перевести в другой статус.");
        }
        if (status == OrderStatus.DELIVERED) {
            stock.deliver(order.getId());
        } else if (OrderStock.reservesStock(status) && !OrderStock.reservesStock(order.getStatus())) {
            stock.requireAvailable(stock.quantities(order.getId()), order.getId());
        }
    }
}
