package com.team.corporate.repositories.impls;

import com.team.corporate.repositories.Transactions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class HibernateTransactions implements Transactions {
    private static final ThreadLocal<Map<SessionFactory, Session>> SESSIONS = new ThreadLocal<>();
    private final SessionFactory factory;

    public HibernateTransactions(@NotNull SessionFactory factory) {
        this.factory = factory;
    }

    static <T> T fromTransaction(SessionFactory factory, Function<Session, T> action) {
        Map<SessionFactory, Session> sessions = SESSIONS.get();
        Session current = sessions == null ? null : sessions.get(factory);
        if (current != null) {
            try {
                return action.apply(current);
            } catch (RuntimeException | Error e) {
                current.getTransaction().markRollbackOnly();
                throw e;
            }
        }
        return factory.fromTransaction(session -> {
            Map<SessionFactory, Session> active = SESSIONS.get();
            if (active == null) {
                active = new HashMap<>();
                SESSIONS.set(active);
            }
            active.put(factory, session);
            try {
                return action.apply(session);
            } finally {
                active.remove(factory);
                if (active.isEmpty()) {
                    SESSIONS.remove();
                }
            }
        });
    }

    static void inTransaction(SessionFactory factory, Consumer<Session> action) {
        fromTransaction(factory, session -> {
            action.accept(session);
            return null;
        });
    }

    @Override
    public <T> T execute(@NotNull Supplier<T> action) {
        return fromTransaction(factory, session -> action.get());
    }
}
