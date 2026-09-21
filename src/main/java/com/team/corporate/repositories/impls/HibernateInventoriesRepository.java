package com.team.corporate.repositories.impls;

import com.team.corporate.entities.Inventory;
import com.team.corporate.repositories.InventoriesRepository;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateInventoriesRepository implements InventoriesRepository {
    private static final String SELECT = "SELECT i FROM Inventory i JOIN FETCH i.product p LEFT JOIN FETCH p.category JOIN FETCH i.warehouse";

    private final SessionFactory sessionFactory;

    public HibernateInventoriesRepository(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
    }

    @Override
    @NotNull
    public Inventory add(@NotNull Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory must not be null");
        return sessionFactory.fromTransaction(session -> {
            session.persist(inventory);
            return inventory;
        });
    }

    @Override
    @NotNull
    public List<Inventory> getAll() {
        return sessionFactory.fromTransaction(session ->
                session.createSelectionQuery(SELECT, Inventory.class).getResultList());
    }

    @Override
    public @NotNull Optional<Inventory> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null.");
        return sessionFactory.fromTransaction(session -> Optional.ofNullable(session.find(Inventory.class, id)));
    }

    /*Not practical*/
    @Override
    @NotNull
    public Optional<Inventory> getByProductAndWarehouseIds(@NotNull UUID productId, @NotNull UUID warehouseId) {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(warehouseId, "warehouseId must not be null");
        return sessionFactory.fromTransaction(session ->
                session.createSelectionQuery(SELECT + " WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId", Inventory.class)
                        .setParameter("productId", productId)
                        .setParameter("warehouseId", warehouseId)
                        .uniqueResultOptional());
    }

    @Override
    @NotNull
    public List<Inventory> getByProductId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessionFactory.fromTransaction(session ->
                session.createSelectionQuery(SELECT + " WHERE i.product.id = :id", Inventory.class)
                        .setParameter("id", id)
                        .getResultList());
    }

    @Override
    @NotNull
    public List<Inventory> getByWarehouseId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessionFactory.fromTransaction(session ->
                session.createSelectionQuery(SELECT + " WHERE i.warehouse.id = :id", Inventory.class)
                        .setParameter("id", id)
                        .getResultList());
    }

    @Override
    @NotNull
    public Inventory update(@NotNull Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory must not be null");
        if (inventory.getId() == null) {
            throw new IllegalArgumentException("Cannot update an inventory without an ID; use add() first");
        }
        return sessionFactory.fromTransaction(session -> session.merge(inventory));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        sessionFactory.inTransaction(session -> {
            Inventory inventory = session.find(Inventory.class, id);
            if (inventory != null) {
                session.remove(inventory);
            }
        });
    }
}

