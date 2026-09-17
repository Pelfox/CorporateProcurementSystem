package com.team.corporate.repositories.impls;

import com.team.corporate.entities.Category;
import com.team.corporate.repositories.CategoriesRepository;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateCategoriesRepository implements CategoriesRepository {
    private final SessionFactory sessionFactory;

    public HibernateCategoriesRepository(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
    }

    @Override
    @NotNull
    public Category add(@NotNull Category category) {
        Objects.requireNonNull(category, "category must not be null");
        return sessionFactory.fromTransaction(session -> {
            session.persist(category);
            return category;
        });
    }

    @Override
    @NotNull
    public List<Category> getAll() {
        return sessionFactory.fromTransaction(session ->
                session.createSelectionQuery("FROM Category", Category.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<Category> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessionFactory.fromTransaction(session -> Optional.ofNullable(session.find(Category.class, id)));
    }

    @Override
    @NotNull
    public Category update(@NotNull Category category) {
        Objects.requireNonNull(category, "category must not be null");
        if (category.getId() == null) {
            throw new IllegalArgumentException("Cannot update a category without an ID; use add() first");
        }
        return sessionFactory.fromTransaction(session -> session.merge(category));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        sessionFactory.inTransaction(session -> {
            Category category = session.find(Category.class, id);
            if (category != null) {
                session.remove(category);
            }
        });
    }
}

