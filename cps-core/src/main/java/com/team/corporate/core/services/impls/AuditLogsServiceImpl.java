package com.team.corporate.core.services.impls;

import com.team.corporate.core.entities.*;
import com.team.corporate.core.exceptions.OrderNotFoundException;
import com.team.corporate.core.exceptions.UserNotFoundException;
import com.team.corporate.core.repositories.AuditLogsRepository;
import com.team.corporate.core.repositories.AuditLogsRepository.AuditFilter;
import com.team.corporate.core.repositories.OrdersRepository;
import com.team.corporate.core.repositories.UsersRepository;
import com.team.corporate.core.services.AuditLogsService;
import com.team.corporate.core.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AuditLogsServiceImpl implements AuditLogsService {
    private final UsersRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final AuditLogsRepository auditLogsRepository;

    public AuditLogsServiceImpl(@NotNull UsersRepository usersRepository,
                                @NotNull OrdersRepository ordersRepository,
                                @NotNull AuditLogsRepository auditLogsRepository) {
        this.ordersRepository = ordersRepository;
        this.userRepository = usersRepository;
        this.auditLogsRepository = auditLogsRepository;
    }

    @Override
    @NotNull
    public AuditLog record(@NotNull EntityType entityType, @NotNull UUID entityUuid,
                           @NotNull AuditEventType eventType, @NotNull UUID changedByUserId,
                           @NotNull String description, @Nullable String correlationId) {
        EntityValidation.requireNonNull(entityType, "Тип сущности");
        EntityValidation.requireNonNull(entityUuid, "UUID сущности");
        EntityValidation.requireNonNull(eventType, "Тип события");
        EntityValidation.requireNonNull(changedByUserId, "Идентификатор автора");
        EntityValidation.requireText(description, "Описание события аудита");
        User changedByUser = userRepository.getById(changedByUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
        return auditLogsRepository.add(new AuditLog(entityType, entityUuid, eventType,
                changedByUser, description, correlationId));
    }

    @Override
    @NotNull
    public AuditLog createAuditLog(@NotNull UUID orderId, @NotNull UUID changedByUserId,
                                   @NotNull OrderStatus oldStatus, @NotNull OrderStatus newStatus) {
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        EntityValidation.requireNonNull(changedByUserId, "Идентификатор автора");
        EntityValidation.requireNonNull(oldStatus, "Предыдущий статус");
        EntityValidation.requireNonNull(newStatus, "Новый статус");
        Order order = ordersRepository.getById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Заказ не найден."));
        User changedByUser = userRepository.getById(changedByUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
        return auditLogsRepository.add(new AuditLog(order, changedByUser, oldStatus, newStatus,
                com.team.corporate.core.utils.CorrelationIdHolder.get()));
    }

    @Override
    @NotNull
    public Optional<AuditLog> getAuditLog(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return auditLogsRepository.getById(id);
    }

    @Override
    @NotNull
    public List<AuditLog> getAllByOrder(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return auditLogsRepository.getByOrderId(id);
    }

    @Override
    @NotNull
    public List<AuditLog> getAllByChangedBy(@NotNull UUID changedByUserId) {
        EntityValidation.requireNonNull(changedByUserId, "Идентификатор автора");
        return auditLogsRepository.getByUserId(changedByUserId);
    }

    @Override
    @NotNull
    public List<AuditLog> getAllByOldStatus(@NotNull OrderStatus status) {
        return auditLogsRepository.getAll();
    }

    @Override
    @NotNull
    public List<AuditLog> getAllByNewStatus(@NotNull OrderStatus status) {
        return auditLogsRepository.getAll();
    }

    @Override
    @NotNull
    public AuditPage search(@NotNull AuditQuery query) {
        EntityValidation.requireNonNull(query, "Запрос аудита");
        AuditFilter filter = new AuditFilter(query.entityType(), query.entityUuid(),
                query.eventType(), query.changedByUserId(), query.textQuery(),
                query.dateFrom(), query.dateTo(), query.correlationId());
        List<AuditLog> items = auditLogsRepository.search(filter, query.sortField(),
                query.sortDesc(), query.offset(), query.size());
        long total = auditLogsRepository.count(filter);
        return new AuditPage(items, total);
    }
}