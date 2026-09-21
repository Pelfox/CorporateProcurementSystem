package com.team.corporate.repositories.impls;

import com.team.corporate.entities.OrderItem;
import com.team.corporate.repositories.OrderItemsRepository;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateOrderItemsRepository implements OrderItemsRepository {
    private static final String SELECT = "SELECT i FROM OrderItem i JOIN FETCH i.order o JOIN FETCH o.user JOIN FETCH i.product p LEFT JOIN FETCH p.category";

    private final SessionFactory sessionFactory;

    public HibernateOrderItemsRepository(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
    }

    @Override
    @NotNull
    public OrderItem add(@NotNull OrderItem orderItem) {
        Objects.requireNonNull(orderItem, "orderItem must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
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
        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
            items.forEach(session::persist);
            return items;
        });
    }

    @Override
    @NotNull
    public List<OrderItem> getAll() {
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT, OrderItem.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<OrderItem> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT + " WHERE i.id = :id", OrderItem.class)
                        .setParameter("id", id)
                        .uniqueResultOptional());
    }

    @Override
    @NotNull
    public List<OrderItem> getByOrderId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT + " WHERE i.order.id = :id", OrderItem.class)
                        .setParameter("id", id)
                        .getResultList());
    }

    @Override
    @NotNull
    public List<OrderItem> getByProductId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT + " WHERE i.product.id = :id", OrderItem.class)
                        .setParameter("id", id)
                        .getResultList());
    }

    @Override
    @NotNull
    public OrderItem update(@NotNull OrderItem orderItem) {
        Objects.requireNonNull(orderItem, "orderItem must not be null");
        if (orderItem.getId() == null) {
            throw new IllegalArgumentException("Cannot update an order item without an ID; use add() first");
        }
        return HibernateTransactions.fromTransaction(sessionFactory, session -> session.merge(orderItem));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        HibernateTransactions.inTransaction(sessionFactory, session -> {
            OrderItem orderItem = session.find(OrderItem.class, id);
            if (orderItem != null) {
                session.remove(orderItem);
            }
        });
    }
}
