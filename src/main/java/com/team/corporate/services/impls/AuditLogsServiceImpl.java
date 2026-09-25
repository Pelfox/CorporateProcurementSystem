package com.team.corporate.services.impls;

import com.team.corporate.entities.AuditLog;
import com.team.corporate.entities.Order;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.entities.User;
import com.team.corporate.exceptions.OrderNotFoundException;
import com.team.corporate.exceptions.UserNotFoundException;
import com.team.corporate.repositories.AuditLogsRepository;
import com.team.corporate.repositories.OrdersRepository;
import com.team.corporate.repositories.UsersRepository;
import com.team.corporate.services.AuditLogsService;
import com.team.corporate.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;

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
    public AuditLog createAuditLog(@NotNull UUID orderId,
                                   @NotNull UUID changedByUserId,
                                   @NotNull OrderStatus oldStatus,
                                   @NotNull OrderStatus newStatus) {
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        EntityValidation.requireNonNull(changedByUserId, "Идентификатор автора изменения");
        EntityValidation.requireNonNull(oldStatus, "Предыдущий статус");
        EntityValidation.requireNonNull(newStatus, "Новый статус");
        Order order = ordersRepository.getById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Заказ с указанным идентификатором не найден."));
        User changedByUser = userRepository.getById(changedByUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с указанным идентификатором не найден."));
        AuditLog auditLog = new AuditLog(order, changedByUser, oldStatus, newStatus);
        return auditLogsRepository.add(auditLog);
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
        EntityValidation.requireNonNull(changedByUserId, "Идентификатор автора изменения");
        return auditLogsRepository.getByUserId(changedByUserId);
    }

    @Override
    @NotNull
    public List<AuditLog> getAllByOldStatus(@NotNull OrderStatus status) {
        EntityValidation.requireNonNull(status, "Статус");
        return auditLogsRepository.getAll();
    }

    @Override
    @NotNull
    public List<AuditLog> getAllByNewStatus(@NotNull OrderStatus status) {
        EntityValidation.requireNonNull(status, "Статус");
        return auditLogsRepository.getAll();
    }
}
