package com.team.corporate.repositories.impls;

import com.team.corporate.entities.User;
import com.team.corporate.repositories.UsersRepository;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateUsersRepository implements UsersRepository {
    private final SessionFactory sessionFactory;

    public HibernateUsersRepository(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
    }

    @Override
    @NotNull
    public User add(@NotNull User user) {
        Objects.requireNonNull(user, "user must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
            session.persist(user);
            return user;
        });
    }

    @Override
    @NotNull
    public List<User> getAll() {
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery("FROM User", User.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<User> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session -> Optional.ofNullable(session.find(User.class, id)));
    }

    @Override
    @NotNull
    public User update(@NotNull User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (user.getId() == null) {
            throw new IllegalArgumentException("Cannot update a user without an ID; use add() first");
        }
        return HibernateTransactions.fromTransaction(sessionFactory, session -> session.merge(user));
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        HibernateTransactions.inTransaction(sessionFactory, session -> {
            User user = session.find(User.class, id);
            if (user != null) {
                session.remove(user);
            }
        });
    }
}
