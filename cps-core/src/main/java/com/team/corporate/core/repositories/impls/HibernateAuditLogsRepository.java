package com.team.corporate.core.repositories.impls;

import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.repositories.AuditLogsRepository;
import com.team.corporate.core.repositories.SessionOperations;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateAuditLogsRepository implements AuditLogsRepository {
    private static final String SELECT = "SELECT a FROM AuditLog a JOIN FETCH a.order o JOIN FETCH o.user JOIN FETCH a.changedBy";

    private final SessionOperations sessions;

    public HibernateAuditLogsRepository(@NotNull SessionFactory sessionFactory) {
        this(new HibernateTransactions(Objects.requireNonNull(sessionFactory, "sessionFactory must not be null")));
    }

    public HibernateAuditLogsRepository(@NotNull SessionOperations sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions must not be null");
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        sessions.inSession(session -> {
            AuditLog log = session.find(AuditLog.class, id);
            if (log != null) {
                session.remove(log);
            }
        });
    }

    @Override
    @NotNull
    public AuditLog add(@NotNull AuditLog auditLog) {
        Objects.requireNonNull(auditLog, "auditLog must not be null");
        return sessions.withSession(session -> {
            session.persist(auditLog);
            return auditLog;
        });
    }

    @Override
    @NotNull
    public List<AuditLog> getAll() {
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT, AuditLog.class).getResultList());
    }

    @Override
    @NotNull
    public Optional<AuditLog> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT + " WHERE a.id = :id", AuditLog.class)
                        .setParameter("id", id)
                        .uniqueResultOptional());
    }

    @Override
    @NotNull
    public List<AuditLog> getByOrderId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT + " WHERE a.order.id = :id", AuditLog.class)
                        .setParameter("id", id)
                        .getResultList());
    }

    @Override
    @NotNull
    public List<AuditLog> getByUserId(@NotNull UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return sessions.withSession(session ->
                session.createSelectionQuery(SELECT + " WHERE a.changedBy.id = :id", AuditLog.class)
                        .setParameter("id", id)
                        .getResultList());
    }
}
