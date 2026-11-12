package com.team.corporate.core.repositories.impls;

import com.team.corporate.core.entities.Warehouse;
import com.team.corporate.core.repositories.WarehousesRepository;
import com.team.corporate.core.repositories.SessionOperations;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateWarehousesRepository implements WarehousesRepository {
    private final SessionOperations sessions;

    public HibernateWarehousesRepository(@NotNull SessionFactory sessionFactory) {
        this(new HibernateTransactions(Objects.requireNonNull(sessionFactory, "sessionFactory must not be null")));
    }

    public HibernateWarehousesRepository(@NotNull SessionOperations sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions must not be null");
    }

    @Override
    @NotNull
    public Warehouse add(@NotNull Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse must not be null");
        return sessions.withSession(session -> {
            session.persist(warehouse);
            return warehouse;
        });
    }

    @Override
    @NotNull
    public List<Warehouse> getAll() {
        return sessions.withSession(session ->
                session.createSelectionQuery("FROM Warehouse", Warehouse.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<Warehouse> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session -> Optional.ofNullable(session.find(Warehouse.class, id)));
    }

    @Override
    @NotNull
    public Warehouse update(@NotNull Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse must not be null");
        if (warehouse.getId() == null) {
            throw new IllegalArgumentException("Нельзя обновить запись без идентификатора. Сначала сохраните её.");
        }
        return sessions.withSession(session -> session.merge(warehouse));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        sessions.inSession(session -> {
            Warehouse warehouse = session.find(Warehouse.class, id);
            if (warehouse != null) {
                session.remove(warehouse);
            }
        });
    }
}
