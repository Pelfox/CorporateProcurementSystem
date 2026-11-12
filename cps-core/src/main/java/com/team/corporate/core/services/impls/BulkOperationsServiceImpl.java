package com.team.corporate.core.services.impls;

import com.team.corporate.core.entities.*;
import com.team.corporate.core.exceptions.UserNotFoundException;
import com.team.corporate.core.repositories.*;
import com.team.corporate.core.services.AuditLogsService;
import com.team.corporate.core.services.BulkOperationsService;
import com.team.corporate.core.services.OrdersService;
import com.team.corporate.core.services.TagsService;
import com.team.corporate.core.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class BulkOperationsServiceImpl implements BulkOperationsService {
    private static final Logger LOG = LoggerFactory.getLogger(BulkOperationsServiceImpl.class);
    private static final int MAX_BULK_SIZE = 500;

    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;
    private final AuditLogsService auditLogsService;
    private final TagsService tagsService;
    private final OrdersService ordersService;
    private final Transactions transactions;

    public BulkOperationsServiceImpl(@NotNull OrdersRepository ordersRepository,
                                     @NotNull UsersRepository usersRepository,
                                     @NotNull AuditLogsService auditLogsService,
                                     @NotNull TagsService tagsService,
                                     @NotNull OrdersService ordersService,
                                     @NotNull Transactions transactions) {
        this.ordersRepository = ordersRepository;
        this.usersRepository = usersRepository;
        this.auditLogsService = auditLogsService;
        this.tagsService = tagsService;
        this.ordersService = ordersService;
        this.transactions = transactions;
    }

    @Override
    public void archive(@NotNull UUID actingUserId, @NotNull UUID orderId, @Nullable String correlationId) {
        EntityValidation.requireNonNull(actingUserId, "Идентификатор пользователя");
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        transactions.execute(() -> {
            requireUserExists(actingUserId);
            Order order = ordersRepository.getByIdForUpdate(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден."));
            if (order.isArchived()) throw new IllegalArgumentException("Заказ уже в архиве.");
            order.setArchived(true);
            order.setArchivedAt(Instant.now());
            ordersRepository.update(order);
            auditLogsService.record(EntityType.ORDER, orderId, AuditEventType.ARCHIVED,
                    actingUserId, "Заказ архивирован.", correlationId);
        });
    }

    @Override
    public void restore(@NotNull UUID actingUserId, @NotNull UUID orderId, @Nullable String correlationId) {
        EntityValidation.requireNonNull(actingUserId, "Идентификатор пользователя");
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        transactions.execute(() -> {
            requireUserExists(actingUserId);
            Order order = ordersRepository.getByIdForUpdate(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден."));
            if (!order.isArchived()) throw new IllegalArgumentException("Заказ не в архиве.");
            order.setArchived(false);
            order.setArchivedAt(null);
            ordersRepository.update(order);
            auditLogsService.record(EntityType.ORDER, orderId, AuditEventType.RESTORED,
                    actingUserId, "Заказ восстановлен из архива.", correlationId);
        });
    }

    @Override
    public void permanentDelete(@NotNull UUID actingUserId, @NotNull UUID orderId, @Nullable String correlationId) {
        EntityValidation.requireNonNull(actingUserId, "Идентификатор пользователя");
        EntityValidation.requireNonNull(orderId, "Идентификатор заказа");
        transactions.execute(() -> {
            User actor = requireManager(actingUserId);
            ordersRepository.getById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Заказ не найден."));
            ordersService.deleteOrder(orderId);
            auditLogsService.record(EntityType.ORDER, orderId, AuditEventType.PERMANENTLY_DELETED,
                    actingUserId, "Заказ окончательно удалён администратором " + actor.getUsername() + ".",
                    correlationId);
        });
    }

    @Override
    @NotNull
    public BulkResult bulkArchive(@NotNull UUID actingUserId, @NotNull List<UUID> orderIds, @Nullable String correlationId) {
        EntityValidation.requireNonNull(actingUserId, "Идентификатор пользователя");
        List<UUID> ids = validateAndDeduplicate(orderIds);
        BulkResultBuilder result = BulkResult.builder();
        for (UUID id : ids) {
            try {
                archive(actingUserId, id, correlationId);
                result.success(id);
            } catch (RuntimeException e) {
                result.failure(id, e.getMessage());
            }
        }
        return result.build();
    }

    @Override
    @NotNull
    public BulkResult bulkRestore(@NotNull UUID actingUserId, @NotNull List<UUID> orderIds, @Nullable String correlationId) {
        EntityValidation.requireNonNull(actingUserId, "Идентификатор пользователя");
        List<UUID> ids = validateAndDeduplicate(orderIds);
        BulkResultBuilder result = BulkResult.builder();
        for (UUID id : ids) {
            try {
                restore(actingUserId, id, correlationId);
                result.success(id);
            } catch (RuntimeException e) {
                result.failure(id, e.getMessage());
            }
        }
        return result.build();
    }

    @Override
    @NotNull
    public BulkResult bulkChangeStatus(@NotNull UUID managerId, @NotNull List<UUID> orderIds,
                                       @NotNull OrderStatus newStatus, @Nullable String correlationId) {
        EntityValidation.requireNonNull(managerId, "Идентификатор менеджера");
        EntityValidation.requireNonNull(newStatus, "Статус");
        List<UUID> ids = validateAndDeduplicate(orderIds);
        BulkResultBuilder result = BulkResult.builder();
        for (UUID id : ids) {
            try {
                ordersService.changeStatus(managerId, id, newStatus);
                result.success(id);
            } catch (RuntimeException e) {
                result.failure(id, e.getMessage());
            }
        }
        return result.build();
    }

    @Override
    @NotNull
    public BulkResult bulkAddTag(@NotNull UUID actingUserId, @NotNull List<UUID> orderIds,
                                 @NotNull String tagName, @Nullable String correlationId) {
        EntityValidation.requireNonNull(actingUserId, "Идентификатор пользователя");
        EntityValidation.requireText(tagName, "Название тега");
        List<UUID> ids = validateAndDeduplicate(orderIds);
        BulkResultBuilder result = BulkResult.builder();
        for (UUID id : ids) {
            try {
                transactions.execute(() -> {
                    requireUserExists(actingUserId);
                    Order order = ordersRepository.getById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Заказ не найден."));
                    if (order.isArchived())
                        throw new IllegalArgumentException("Нельзя добавить тег архивному заказу.");
                    tagsService.attachToOrder(id, tagName, null);
                    auditLogsService.record(EntityType.ORDER, id, AuditEventType.BULK_TAG_ADDED,
                            actingUserId, "К заказу добавлен тег «" + tagName + "».", correlationId);
                });
                result.success(id);
            } catch (RuntimeException e) {
                result.failure(id, e.getMessage());
            }
        }
        return result.build();
    }

    @Override
    @NotNull
    public BulkResult bulkRemoveTag(@NotNull UUID actingUserId, @NotNull List<UUID> orderIds,
                                    @NotNull String tagName, @Nullable String correlationId) {
        EntityValidation.requireNonNull(actingUserId, "Идентификатор пользователя");
        EntityValidation.requireText(tagName, "Название тега");
        List<UUID> ids = validateAndDeduplicate(orderIds);
        BulkResultBuilder result = BulkResult.builder();
        for (UUID id : ids) {
            try {
                transactions.execute(() -> {
                    requireUserExists(actingUserId);
                    ordersRepository.getById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Заказ не найден."));
                    boolean removed = tagsService.detachFromOrder(id, tagName);
                    if (!removed)
                        throw new IllegalArgumentException("Тег «" + tagName + "» не был привязан к заказу.");
                    auditLogsService.record(EntityType.ORDER, id, AuditEventType.BULK_TAG_REMOVED,
                            actingUserId, "С заказа снят тег «" + tagName + "».", correlationId);
                });
                result.success(id);
            } catch (RuntimeException e) {
                result.failure(id, e.getMessage());
            }
        }
        return result.build();
    }

    private void requireUserExists(@NotNull UUID userId) {
        if (usersRepository.getById(userId).isEmpty())
            throw new UserNotFoundException("Пользователь не найден.");
    }

    @NotNull
    private User requireManager(@NotNull UUID userId) {
        User user = usersRepository.getById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден."));
        if (!user.getUserRole().canManageProcurement())
            throw new IllegalArgumentException("Требуется роль менеджера.");
        return user;
    }

    @NotNull
    private List<UUID> validateAndDeduplicate(@NotNull List<UUID> orderIds) {
        EntityValidation.requireNonNull(orderIds, "Список идентификаторов");
        if (orderIds.isEmpty())
            throw new IllegalArgumentException("Список идентификаторов не должен быть пустым.");
        if (orderIds.size() > MAX_BULK_SIZE)
            throw new IllegalArgumentException("Максимум " + MAX_BULK_SIZE + " записей за один запрос.");
        return new ArrayList<>(new LinkedHashSet<>(orderIds));
    }

    public static final class BulkResultBuilder {
        private final List<UUID> succeeded = new ArrayList<>();
        private final Map<UUID, String> failed = new LinkedHashMap<>();
        public void success(UUID id) { succeeded.add(id); }
        public void failure(UUID id, String message) { failed.put(id, message == null ? "Неизвестная ошибка." : message); }
        public BulkResult build() { return new BulkResult(List.copyOf(succeeded), Map.copyOf(failed)); }
    }
}