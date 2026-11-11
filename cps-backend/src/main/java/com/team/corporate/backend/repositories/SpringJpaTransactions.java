package com.team.corporate.backend.repositories;

import com.team.corporate.core.repositories.SessionOperations;
import com.team.corporate.core.repositories.Transactions;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Function;
import java.util.function.Supplier;

/** Shares the Spring-managed EntityManager with Spring Data repositories. */
@Component
public class SpringJpaTransactions implements Transactions, SessionOperations {
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;

    public SpringJpaTransactions(EntityManager entityManager, PlatformTransactionManager manager) {
        this.entityManager = entityManager;
        this.transactions = new TransactionTemplate(manager);
    }

    @Override
    public <T> T execute(@NotNull Supplier<T> action) {
        return transactions.execute(status -> action.get());
    }

    @Override
    public <T> T withSession(Function<Session, T> action) {
        return execute(() -> action.apply(entityManager.unwrap(Session.class)));
    }
}
