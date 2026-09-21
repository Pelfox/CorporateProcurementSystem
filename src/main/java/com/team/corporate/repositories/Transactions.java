package com.team.corporate.repositories;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface Transactions {
    <T> T execute(@NotNull Supplier<T> action);

    default void execute(@NotNull Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }
}
