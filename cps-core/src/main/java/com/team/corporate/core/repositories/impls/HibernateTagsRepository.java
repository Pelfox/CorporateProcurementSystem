package com.team.corporate.core.repositories.impls;

import com.team.corporate.core.entities.OrderTag;
import com.team.corporate.core.repositories.TagsRepository;
import com.team.corporate.core.repositories.SessionOperations;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HibernateTagsRepository implements TagsRepository {
    private final SessionOperations sessions;

    public HibernateTagsRepository(@NotNull SessionFactory sessionFactory) {
        this(new HibernateTransactions(Objects.requireNonNull(sessionFactory)));
    }

    public HibernateTagsRepository(@NotNull SessionOperations sessions) {
        this.sessions = Objects.requireNonNull(sessions);
    }

    @Override
    @NotNull
    public OrderTag add(@NotNull OrderTag tag) {
        Objects.requireNonNull(tag);
        return sessions.withSession(session -> {
            session.persist(tag);
            return tag;
        });
    }

    @Override
    @NotNull
    public Optional<OrderTag> getById(@NotNull UUID id) {
        Objects.requireNonNull(id);
        return sessions.withSession(session ->
                Optional.ofNullable(session.find(OrderTag.class, id)));
    }

    @Override
    @NotNull
    public Optional<OrderTag> getByName(@NotNull String name) {
        Objects.requireNonNull(name);
        return sessions.withSession(session ->
                session.createSelectionQuery("FROM OrderTag t WHERE lower(t.name) = lower(:name)", OrderTag.class)
                        .setParameter("name", name.strip()).uniqueResultOptional());
    }

    @Override
    @NotNull
    public List<OrderTag> getAll() {
        return sessions.withSession(session ->
                session.createSelectionQuery("FROM OrderTag t ORDER BY t.name", OrderTag.class).getResultList());
    }

    @Override
    public void delete(@NotNull UUID id) {
        Objects.requireNonNull(id);
        sessions.inSession(session -> {
            OrderTag tag = session.find(OrderTag.class, id);
            if (tag != null) session.remove(tag);
        });
    }

    @Override
    @NotNull
    public OrderTag attachToOrder(@NotNull UUID orderId, @NotNull String tagName, @Nullable String color) {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(tagName);
        return sessions.withSession(session -> {
            OrderTag tag = session.createSelectionQuery(
                            "FROM OrderTag t WHERE lower(t.name) = lower(:name)", OrderTag.class)
                    .setParameter("name", tagName.strip())
                    .uniqueResultOptional()
                    .orElseGet(() -> {
                        OrderTag newTag = new OrderTag(tagName.strip(), color == null ? "" : color);
                        session.persist(newTag);
                        return newTag;
                    });
            long exists = session.createNativeQuery(
                            "SELECT count(*) FROM order_tags WHERE order_id = :orderId AND tag_id = :tagId", Long.class)
                    .setParameter("orderId", orderId).setParameter("tagId", tag.getId()).getSingleResult();
            if (exists == 0) {
                session.createNativeMutationQuery(
                                "INSERT INTO order_tags (order_id, tag_id) VALUES (:orderId, :tagId)")
                        .setParameter("orderId", orderId).setParameter("tagId", tag.getId()).executeUpdate();
            }
            return tag;
        });
    }

    @Override
    public boolean detachFromOrder(@NotNull UUID orderId, @NotNull String tagName) {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(tagName);
        return sessions.withSession(session -> {
            int affected = session.createNativeMutationQuery(
                            "DELETE FROM order_tags ot USING tags t " +
                                    "WHERE ot.tag_id = t.id AND ot.order_id = :orderId " +
                                    "AND lower(t.name) = lower(:tagName)")
                    .setParameter("orderId", orderId).setParameter("tagName", tagName.strip())
                    .executeUpdate();
            return affected > 0;
        });
    }

    @Override
    @NotNull
    public List<OrderTag> getByOrderId(@NotNull UUID orderId) {
        Objects.requireNonNull(orderId);
        return sessions.withSession(session ->
                session.createNativeQuery(
                                "SELECT t.* FROM tags t JOIN order_tags ot ON t.id = ot.tag_id " +
                                        "WHERE ot.order_id = :orderId ORDER BY t.name", OrderTag.class)
                        .setParameter("orderId", orderId).getResultList());
    }

    @Override
    @NotNull
    public List<UUID> getOrderIdsByTag(@NotNull String tagName) {
        Objects.requireNonNull(tagName);
        return sessions.withSession(session ->
                session.createNativeQuery(
                                "SELECT ot.order_id FROM order_tags ot JOIN tags t ON ot.tag_id = t.id " +
                                        "WHERE lower(t.name) = lower(:tagName)", UUID.class)
                        .setParameter("tagName", tagName.strip()).getResultList());
    }
}
