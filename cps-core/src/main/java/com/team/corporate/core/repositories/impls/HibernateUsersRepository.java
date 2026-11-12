package com.team.corporate.core.repositories.impls;

import com.team.corporate.core.entities.User;
import com.team.corporate.core.repositories.UsersRepository;
import com.team.corporate.core.repositories.SessionOperations;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateUsersRepository implements UsersRepository {
    private final SessionOperations sessions;

    public HibernateUsersRepository(@NotNull SessionFactory sessionFactory) {
        this(new HibernateTransactions(Objects.requireNonNull(sessionFactory, "sessionFactory must not be null")));
    }

    public HibernateUsersRepository(@NotNull SessionOperations sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions must not be null");
    }

    @Override
    @NotNull
    public User add(@NotNull User user) {
        Objects.requireNonNull(user, "user must not be null");
        return sessions.withSession(session -> {
            session.persist(user);
            return user;
        });
    }

    @Override
    @NotNull
    public List<User> getAll() {
        return sessions.withSession(session ->
                session.createSelectionQuery("FROM User", User.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<User> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session -> Optional.ofNullable(session.find(User.class, id)));
    }

    @Override
    @NotNull
    public User update(@NotNull User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (user.getId() == null) {
            throw new IllegalArgumentException("Нельзя обновить запись без идентификатора. Сначала сохраните её.");
        }
        return sessions.withSession(session -> session.merge(user));
    }

    @Override
    @NotNull
    public Optional<User> getByUsername(@NotNull String username) {
        Objects.requireNonNull(username, "username must not be null");
        return sessions.withSession(session -> session
                .createSelectionQuery("from User u where u.username = :username", User.class)
                .setParameter("username", username).uniqueResultOptional());
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        sessions.inSession(session -> {
            User user = session.find(User.class, id);
            if (user != null) {
                session.remove(user);
            }
        });
    }
}
