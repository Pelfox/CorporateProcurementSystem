package com.team.corporate.backend.http;

import com.team.corporate.core.repositories.Transactions;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ApiExecutor {
    private final Transactions transactions;

    public ApiExecutor(@NotNull Transactions transactions) {
        this.transactions = transactions;
    }

    public <T> T execute(@NotNull Supplier<T> action) {
        return transactions.execute(action);
    }

    public void run(@NotNull Runnable action) {
        transactions.execute(action);
    }
}
