package com.team.corporate.repositories.impls;

import com.team.corporate.entities.AuditLog;
import com.team.corporate.repositories.AuditLogsRepository;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateAuditLogsRepository implements AuditLogsRepository {
    private static final String SELECT = "SELECT a FROM AuditLog a JOIN FETCH a.order o JOIN FETCH o.user JOIN FETCH a.changedBy";

    private final SessionFactory sessionFactory;

    public HibernateAuditLogsRepository(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory must not be null");
    }

    @Override
    @NotNull
    public AuditLog add(@NotNull AuditLog auditLog) {
        Objects.requireNonNull(auditLog, "auditLog must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
            session.persist(auditLog);
            return auditLog;
        });
    }

    @Override
    @NotNull
    public List<AuditLog> getAll() {
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT, AuditLog.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<AuditLog> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT + " WHERE a.id = :id", AuditLog.class)
                        .setParameter("id", id)
                        .uniqueResultOptional());
    }

    @Override
    @NotNull
    public List<AuditLog> getByOrderId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT + " WHERE a.order.id = :id", AuditLog.class)
                        .setParameter("id", id)
                        .getResultList());
    }

    @Override
    @NotNull
    public List<AuditLog> getByUserId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery(SELECT + " WHERE a.changedBy.id = :id", AuditLog.class)
                        .setParameter("id", id)
                        .getResultList());
    }
}
