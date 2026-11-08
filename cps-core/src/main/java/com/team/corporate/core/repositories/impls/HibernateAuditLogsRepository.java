package com.team.corporate.core.repositories.impls;

import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.repositories.AuditLogsRepository;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class HibernateAuditLogsRepository implements AuditLogsRepository {
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "changedAt", "entityType", "eventType", "changedBy");

    private final SessionFactory sessionFactory;

    public HibernateAuditLogsRepository(@NotNull SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory);
    }

    @Override
    @NotNull
    public AuditLog add(@NotNull AuditLog auditLog) {
        Objects.requireNonNull(auditLog);
        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
            session.persist(auditLog);
            return auditLog;
        });
    }

    @Override
    @NotNull
    public Optional<AuditLog> getById(@NotNull UUID id) {
        Objects.requireNonNull(id);
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery("SELECT a FROM AuditLog a LEFT JOIN FETCH a.order o " +
                                "LEFT JOIN FETCH o.user JOIN FETCH a.changedBy WHERE a.id = :id", AuditLog.class)
                        .setParameter("id", id).uniqueResultOptional());
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id);
        HibernateTransactions.inTransaction(sessionFactory, session -> {
            AuditLog log = session.find(AuditLog.class, id);
            if (log != null) session.remove(log);
        });
    }

    @Override
    @NotNull
    public List<AuditLog> getAll() {
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery("SELECT a FROM AuditLog a LEFT JOIN FETCH a.order o " +
                        "LEFT JOIN FETCH o.user JOIN FETCH a.changedBy", AuditLog.class).getResultList());
    }

    @Override
    @NotNull
    public List<AuditLog> getByOrderId(@NotNull UUID id) {
        Objects.requireNonNull(id);
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery("SELECT a FROM AuditLog a LEFT JOIN FETCH a.order o " +
                                "LEFT JOIN FETCH o.user JOIN FETCH a.changedBy WHERE a.order.id = :id OR a.entityUuid = :id",
                        AuditLog.class).setParameter("id", id).getResultList());
    }

    @Override
    @NotNull
    public List<AuditLog> getByUserId(@NotNull UUID id) {
        Objects.requireNonNull(id);
        return HibernateTransactions.fromTransaction(sessionFactory, session ->
                session.createSelectionQuery("SELECT a FROM AuditLog a LEFT JOIN FETCH a.order o " +
                                "LEFT JOIN FETCH o.user JOIN FETCH a.changedBy WHERE a.changedBy.id = :id",
                        AuditLog.class).setParameter("id", id).getResultList());
    }

    @Override
    @NotNull
    public List<AuditLog> search(@NotNull AuditFilter filter, @NotNull String sortField,
                                 boolean sortDesc, int offset, int limit) {
        Objects.requireNonNull(filter);
        String safeSort = SORTABLE_FIELDS.contains(sortField) ? sortField : "changedAt";
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(limit, 200));

        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<AuditLog> cq = cb.createQuery(AuditLog.class);
            Root<AuditLog> root = cq.from(AuditLog.class);
            root.fetch("changedBy");
            cq.where(buildPredicates(filter, cb, root).toArray(new Predicate[0]));
            var sortPath = root.get(safeSort);
            cq.orderBy(sortDesc ? cb.desc(sortPath) : cb.asc(sortPath));
            TypedQuery<AuditLog> query = session.createQuery(cq);
            query.setFirstResult(safeOffset);
            query.setMaxResults(safeLimit);
            return query.getResultList();
        });
    }

    @Override
    public long count(@NotNull AuditFilter filter) {
        Objects.requireNonNull(filter);
        return HibernateTransactions.fromTransaction(sessionFactory, session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<AuditLog> root = cq.from(AuditLog.class);
            cq.select(cb.count(root));
            cq.where(buildPredicates(filter, cb, root).toArray(new Predicate[0]));
            return session.createQuery(cq).getSingleResult();
        });
    }

    private List<Predicate> buildPredicates(AuditFilter filter, CriteriaBuilder cb, Root<AuditLog> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (filter.entityType() != null) predicates.add(cb.equal(root.get("entityType"), filter.entityType()));
        if (filter.entityUuid() != null) predicates.add(cb.equal(root.get("entityUuid"), filter.entityUuid()));
        if (filter.eventType() != null) predicates.add(cb.equal(root.get("eventType"), filter.eventType()));
        if (filter.changedByUserId() != null)
            predicates.add(cb.equal(root.get("changedBy").get("id"), filter.changedByUserId()));
        if (filter.dateFrom() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("changedAt"), filter.dateFrom()));
        if (filter.dateTo() != null) predicates.add(cb.lessThanOrEqualTo(root.get("changedAt"), filter.dateTo()));
        if (filter.correlationId() != null) predicates.add(cb.equal(root.get("correlationId"), filter.correlationId()));
        if (filter.textQuery() != null) {
            String pattern = "%" + filter.textQuery() + "%";
            predicates.add(cb.like(cb.lower(root.get("description")), pattern));
        }
        return predicates;
    }
}