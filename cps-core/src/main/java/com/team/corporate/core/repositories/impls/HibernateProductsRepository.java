package com.team.corporate.core.repositories.impls;

import com.team.corporate.core.entities.Product;
import com.team.corporate.core.repositories.ProductsRepository;
import jakarta.persistence.LockModeType;
import com.team.corporate.core.repositories.SessionOperations;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateProductsRepository implements ProductsRepository {
    private static final String SELECT = "SELECT p FROM Product p LEFT JOIN FETCH p.category";

    private final SessionOperations sessions;

    public HibernateProductsRepository(@NotNull SessionFactory sessionFactory) {
        this(new HibernateTransactions(Objects.requireNonNull(sessionFactory, "sessionFactory must not be null")));
    }

    public HibernateProductsRepository(@NotNull SessionOperations sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions must not be null");
    }

    @Override
    @NotNull
    public Product add(@NotNull Product product) {
        Objects.requireNonNull(product, "product must not be null");
        return sessions.withSession(session -> {
            session.persist(product);
            return product;
        });
    }

    @Override
    @NotNull
    public List<Product> getAll() {
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT, Product.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<Product> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT + " WHERE p.id = :id", Product.class)
                        .setParameter("id", id)
                        .uniqueResultOptional());
    }

    @Override
    public @NotNull Optional<Product> getByIdForUpdate(@NotNull UUID id) {
        return sessions.withSession(session ->
                session.createSelectionQuery("SELECT p FROM Product p WHERE p.id = :id", Product.class)
                        .setParameter("id", id).setLockMode(LockModeType.PESSIMISTIC_WRITE).uniqueResultOptional());
    }

    @Override
    @NotNull
    public Optional<Product> getBySKU(@NotNull String sku) {
        Objects.requireNonNull(sku, "sku must not be null");
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT + " WHERE p.sku = :sku", Product.class)
                        .setParameter("sku", sku)
                        .uniqueResultOptional());
    }

    @Override
    @NotNull
    public Product update(@NotNull Product product) {
        Objects.requireNonNull(product, "product must not be null");
        if (product.getId() == null) {
            throw new IllegalArgumentException("Нельзя обновить запись без идентификатора. Сначала сохраните её.");
        }
        return sessions.withSession(session -> session.merge(product));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        sessions.inSession(session -> {
            Product product = session.find(Product.class, id);
            if (product != null) {
                session.remove(product);
            }
        });
    }
}
