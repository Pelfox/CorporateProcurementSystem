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

    public UsersRepository userRepository;
    public OrdersRepository ordersRepository;
    public AuditLogsRepository auditLogsRepository;

    public AuditLogsServiceImpl(@NotNull UsersRepository usersRepository, @NotNull OrdersRepository ordersRepository, @NotNull AuditLogsRepository auditLogsRepository) {
        this.ordersRepository = Objects.requireNonNull(ordersRepository, "ordersRepository must not be null.");
        this.userRepository = Objects.requireNonNull(usersRepository, "usersRepository must not be null.");
        this.auditLogsRepository = Objects.requireNonNull(auditLogsRepository, "auditLogsRepository must not be null.");
    }

    @Override
    public @NotNull AuditLog createAuditLog(@NotNull UUID orderId, @NotNull UUID changedByUserId, @NotNull OrderStatus oldStatus, @NotNull OrderStatus newStatus) {
        /* Handling possible null value */
        Optional<Order> orderSearchResult = ordersRepository.getById(orderId);
        if (orderSearchResult.isEmpty()){
            throw new OrderNotFoundException();
        }
        Order order = orderSearchResult.get();


        /* Handling possible null value */
        Optional<User> userSearchResult = userRepository.getById(changedByUserId);
        if (userSearchResult.isEmpty()){
            throw new UserNotFoundException();
        }
        /* Creating new auditLog object and adding by repo */
        User changedByUser = userSearchResult.get();
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
        /* Need repository method */
        List<AuditLog> allAuditLogs = auditLogsRepository.getAll();
        return allAuditLogs;
    }

    @Override
    public @NotNull List<AuditLog> getAllByNewStatus(@NotNull OrderStatus status) {
        /* Need repository method */
        List<AuditLog> allAuditLogs = auditLogsRepository.getAll();
        return allAuditLogs;
    }
}
