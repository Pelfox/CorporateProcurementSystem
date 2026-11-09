package com.team.corporate.core.services;

import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.services.impls.BulkOperationsServiceImpl.BulkResultBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BulkOperationsService {
    void archive(@NotNull UUID actingUserId, @NotNull UUID orderId, @Nullable String correlationId);
    void restore(@NotNull UUID actingUserId, @NotNull UUID orderId, @Nullable String correlationId);
    void permanentDelete(@NotNull UUID actingUserId, @NotNull UUID orderId, @Nullable String correlationId);

    @NotNull BulkResult bulkArchive(@NotNull UUID actingUserId, @NotNull List<UUID> orderIds, @Nullable String correlationId);
    @NotNull BulkResult bulkRestore(@NotNull UUID actingUserId, @NotNull List<UUID> orderIds, @Nullable String correlationId);
    @NotNull BulkResult bulkChangeStatus(@NotNull UUID managerId, @NotNull List<UUID> orderIds,
                                         @NotNull OrderStatus newStatus, @Nullable String correlationId);
    @NotNull BulkResult bulkAddTag(@NotNull UUID actingUserId, @NotNull List<UUID> orderIds,
                                   @NotNull String tagName, @Nullable String correlationId);
    @NotNull BulkResult bulkRemoveTag(@NotNull UUID actingUserId, @NotNull List<UUID> orderIds,
                                      @NotNull String tagName, @Nullable String correlationId);

    record BulkResult(@NotNull List<UUID> succeeded, @NotNull Map<UUID, String> failed) {
        public boolean hasFailures() { return !failed.isEmpty(); }
        @NotNull
        public static BulkResultBuilder builder() { return new BulkResultBuilder(); }
    }
}