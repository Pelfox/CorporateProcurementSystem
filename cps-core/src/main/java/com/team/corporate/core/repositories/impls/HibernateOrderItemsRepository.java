package com.team.corporate.core.repositories.impls;

import com.team.corporate.core.entities.OrderItem;
import com.team.corporate.core.repositories.OrderItemsRepository;
import jakarta.persistence.LockModeType;
import com.team.corporate.core.repositories.SessionOperations;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateOrderItemsRepository implements OrderItemsRepository {
    private static final String SELECT = "SELECT i FROM OrderItem i JOIN FETCH i.order o JOIN FETCH o.user JOIN FETCH i.product p LEFT JOIN FETCH p.category";

    private final SessionOperations sessions;

    public HibernateOrderItemsRepository(@NotNull SessionFactory sessionFactory) {
        this(new HibernateTransactions(Objects.requireNonNull(sessionFactory, "sessionFactory must not be null")));
    }

    public HibernateOrderItemsRepository(@NotNull SessionOperations sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions must not be null");
    }

    @Override
    @NotNull
    public OrderItem add(@NotNull OrderItem orderItem) {
        Objects.requireNonNull(orderItem, "orderItem must not be null");
        return sessions.withSession(session -> {
            session.persist(orderItem);
            return orderItem;
        });
    }

    @Override
    @NotNull
    public List<OrderItem> addAll(@NotNull List<OrderItem> orderItems) {
        Objects.requireNonNull(orderItems, "orderItems must not be null");
        List<OrderItem> items = List.copyOf(orderItems);
        if (items.isEmpty()) {
            return items;
        }
        return sessions.withSession(session -> {
            items.forEach(session::persist);
            return items;
        });
    }

    @Override
    @NotNull
    public List<OrderItem> getAll() {
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT, OrderItem.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<OrderItem> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT + " WHERE i.id = :id", OrderItem.class)
                        .setParameter("id", id)
                        .uniqueResultOptional());
    }

    @Override
    public @NotNull Optional<OrderItem> getByIdForUpdate(@NotNull UUID id) {
        return sessions.withSession(session -> {
            session.flush();
            OrderItem item = session.find(OrderItem.class, id);
            if (item != null) session.refresh(item, LockModeType.PESSIMISTIC_WRITE);
            return Optional.ofNullable(item);
        });
    }

    @Override
    @NotNull
    public List<OrderItem> getByOrderId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT + " WHERE i.order.id = :id", OrderItem.class)
                        .setParameter("id", id)
                        .getResultList());
    }

    @Override
    @NotNull
    public List<OrderItem> getByProductId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT + " WHERE i.product.id = :id", OrderItem.class)
                        .setParameter("id", id)
                        .getResultList());
    }

    @Override
    @NotNull
    public OrderItem update(@NotNull OrderItem orderItem) {
        Objects.requireNonNull(orderItem, "orderItem must not be null");
        if (orderItem.getId() == null) {
            throw new IllegalArgumentException("Нельзя обновить запись без идентификатора. Сначала сохраните её.");
        }
        return sessions.withSession(session -> session.merge(orderItem));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        sessions.inSession(session -> {
            OrderItem orderItem = session.find(OrderItem.class, id);
            if (orderItem != null) {
                session.remove(orderItem);
            }
        });
    }
}
