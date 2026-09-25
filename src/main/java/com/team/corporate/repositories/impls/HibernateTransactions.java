package com.team.corporate.repositories.impls;

import com.team.corporate.repositories.Transactions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
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
        try {
            return executeTransaction(factory, action);
        } catch (RuntimeException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql) {
                    String state = sql.getSQLState();
                    String message = switch (state == null ? "" : state) {
                        case "23505" -> "Запись с такими уникальными данными уже существует.";
                        case "23503", "23506" -> "Запись связана с другими данными или уже удалена. Обновите список и повторите действие.";
                        case "23502" -> "Заполните все обязательные поля.";
                        case "22001" -> "Текст превышает допустимую длину.";
                        case "22003" -> "Число выходит за допустимый диапазон.";
                        case "23514" -> "Значение нарушает ограничения данных.";
                        default -> null;
                    };
                    if (message != null) {
                        throw new IllegalArgumentException(message, e);
                    }
                }
            }
            throw e;
        }
    }

    private static <T> T executeTransaction(SessionFactory factory, Function<Session, T> action) {
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
