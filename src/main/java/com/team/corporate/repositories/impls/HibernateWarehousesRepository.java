package com.team.corporate.repositories.impls;

import com.team.corporate.entities.Warehouse;
import com.team.corporate.repositories.WarehousesRepository;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateWarehousesRepository implements WarehousesRepository {
    private final SessionFactory sessionFactory;

    public HibernateWarehousesRepository(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
    }

    @Override
    @NotNull
    public Warehouse add(@NotNull Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
            session.persist(warehouse);
            return warehouse;
        });
    }

    @Override
    @NotNull
    public List<Warehouse> getAll() {
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery("FROM Warehouse", Warehouse.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<Warehouse> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session -> Optional.ofNullable(session.find(Warehouse.class, id)));
    }

    @Override
    @NotNull
    public Warehouse update(@NotNull Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse must not be null");
        if (warehouse.getId() == null) {
            throw new IllegalArgumentException("Cannot update a warehouse without an ID; use add() first");
        }
        return HibernateTransactions.fromTransaction(sessionFactory, session -> session.merge(warehouse));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        HibernateTransactions.inTransaction(sessionFactory, session -> {
            Warehouse warehouse = session.find(Warehouse.class, id);
            if (warehouse != null) {
                session.remove(warehouse);
            }
        });
    }
}
