package com.team.corporate.repositories.impls;

import com.team.corporate.entities.Product;
import com.team.corporate.repositories.ProductsRepository;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateProductsRepository implements ProductsRepository {
    private static final String SELECT = "SELECT p FROM Product p LEFT JOIN FETCH p.category";

    private final SessionFactory sessionFactory;

    public HibernateProductsRepository(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
    }

    @Override
    @NotNull
    public Product add(@NotNull Product product) {
        Objects.requireNonNull(product, "product must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
            session.persist(product);
            return product;
        });
    }

    @Override
    @NotNull
    public List<Product> getAll() {
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT, Product.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<Product> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT + " WHERE p.id = :id", Product.class)
                        .setParameter("id", id)
                        .uniqueResultOptional());
    }

    @Override
    @NotNull
    public Optional<Product> getBySKU(@NotNull String sku) {
        Objects.requireNonNull(sku, "sku must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
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
        return HibernateTransactions.fromTransaction(sessionFactory, session -> session.merge(product));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        HibernateTransactions.inTransaction(sessionFactory, session -> {
            Product product = session.find(Product.class, id);
            if (product != null) {
                session.remove(product);
            }
        });
    }
}
