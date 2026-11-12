package com.team.corporate.core.repositories.impls;

import com.team.corporate.core.entities.Category;
import com.team.corporate.core.repositories.CategoriesRepository;
import com.team.corporate.core.repositories.SessionOperations;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateCategoriesRepository implements CategoriesRepository {
    private final SessionOperations sessions;

    public HibernateCategoriesRepository(@NotNull SessionFactory sessionFactory) {
        this(new HibernateTransactions(Objects.requireNonNull(sessionFactory, "sessionFactory must not be null")));
    }

    public HibernateCategoriesRepository(@NotNull SessionOperations sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions must not be null");
    }

    @Override
    @NotNull
    public Category add(@NotNull Category category) {
        Objects.requireNonNull(category, "category must not be null");
        return sessions.withSession(session -> {
            session.persist(category);
            return category;
        });
    }

    @Override
    @NotNull
    public List<Category> getAll() {
        return sessions.withSession(session ->
                session.createSelectionQuery("FROM Category", Category.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<Category> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session -> Optional.ofNullable(session.find(Category.class, id)));
    }

    @Override
    @NotNull
    public Category update(@NotNull Category category) {
        Objects.requireNonNull(category, "category must not be null");
        if (category.getId() == null) {
            throw new IllegalArgumentException("Нельзя обновить запись без идентификатора. Сначала сохраните её.");
        }
        return sessions.withSession(session -> session.merge(category));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        sessions.inSession(session -> {
            Category category = session.find(Category.class, id);
            if (category != null) {
                session.remove(category);
            }
        });
    }
}
