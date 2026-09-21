package com.team.corporate.repositories.impls;

import com.team.corporate.entities.Order;
import com.team.corporate.repositories.OrdersRepository;
import jakarta.persistence.LockModeType;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateOrdersRepository implements OrdersRepository {
    private static final String SELECT = "SELECT o FROM Order o JOIN FETCH o.user";

    private final SessionFactory sessionFactory;

    public HibernateOrdersRepository(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
    }

    @Override
    @NotNull
    public Order add(@NotNull Order order) {
        Objects.requireNonNull(order, "order must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
            session.persist(order);
            return order;
        });
    }

    @Override
    @NotNull
    public List<Order> getAll() {
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT, Order.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<Order> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT + " WHERE o.id = :id", Order.class)
                        .setParameter("id", id)
                        .uniqueResultOptional());
    }

    @Override
    @NotNull
    public Order update(@NotNull Order order) {
        Objects.requireNonNull(order, "order must not be null");
        if (order.getId() == null) {
            throw new IllegalArgumentException("Cannot update an order without an ID; use add() first");
        }
        return HibernateTransactions.fromTransaction(sessionFactory, session -> session.merge(order));
    }

    @Override
    @NotNull
    public Optional<Order> getByIdForUpdate(@NotNull UUID id) {
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT + " WHERE o.id = :id", Order.class)
                        .setParameter("id", id)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .uniqueResultOptional());
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        HibernateTransactions.inTransaction(sessionFactory, session -> {
            Order order = session.find(Order.class, id);
            if (order != null) {
                session.remove(order);
            }
        });
    }
}
