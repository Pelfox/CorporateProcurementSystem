package com.team.corporate.core.repositories;

import org.hibernate.Session;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runs repository work in the caller's transaction, or starts one if necessary.
 * Legacy clients and the Spring backend supply their own transaction adapters.
 */
public interface SessionOperations {
    <T> T withSession(Function<Session, T> action);

    default void inSession(Consumer<Session> action) {
        withSession(session -> {
            action.accept(session);
            return null;
        });
    }
}
