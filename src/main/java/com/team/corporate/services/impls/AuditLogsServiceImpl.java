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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class AuditLogsServiceImpl implements AuditLogsService {

    private final UsersRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final AuditLogsRepository auditLogsRepository;

    public AuditLogsServiceImpl(@NotNull UsersRepository usersRepository, @NotNull OrdersRepository ordersRepository, @NotNull AuditLogsRepository auditLogsRepository) {
        this.ordersRepository = Objects.requireNonNull(ordersRepository, "ordersRepository must not be null.");
        this.userRepository = Objects.requireNonNull(usersRepository, "usersRepository must not be null.");
        this.auditLogsRepository = Objects.requireNonNull(auditLogsRepository, "auditLogsRepository must not be null.");
    }

    @Override
    public @NotNull AuditLog createAuditLog(@NotNull UUID orderId, @NotNull UUID changedByUserId, @NotNull OrderStatus oldStatus, @NotNull OrderStatus newStatus) {
        Order order = ordersRepository.getById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Заказ с указанным Id не найден."));

        User changedByUser = userRepository.getById(changedByUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользоватеть с указанным Id не найден."));

        AuditLog auditLog = new AuditLog(order, changedByUser, oldStatus, newStatus);
        return auditLogsRepository.add(auditLog);
    }

    @Override
    public @NotNull Optional<AuditLog> getAuditLog(@NotNull UUID id) {
        return auditLogsRepository.getById(id);
    }

    @Override
    public @NotNull List<AuditLog> getAllByOrder(@NotNull UUID id) {
        return auditLogsRepository.getByOrderId(id);
    }

    @Override
    public @NotNull List<AuditLog> getAllByChangedBy(@NotNull UUID changedByUserId) {
        return auditLogsRepository.getByUserId(changedByUserId);
    }

    @Override
    public @NotNull List<AuditLog> getAllByOldStatus(@NotNull OrderStatus status) {

        return auditLogsRepository.getAll();
    }

    @Override
    public @NotNull List<AuditLog> getAllByNewStatus(@NotNull OrderStatus status) {

        return auditLogsRepository.getAll();
    }
}
