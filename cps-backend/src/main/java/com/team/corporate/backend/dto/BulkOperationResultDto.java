package com.team.corporate.backend.dto;

import com.team.corporate.core.services.BulkOperationsService.BulkResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BulkOperationResultDto(
        List<UUID> succeeded, Map<UUID, String> failed,
        int totalRequested, int totalSucceeded, int totalFailed
) {
    public static BulkOperationResultDto from(@NotNull BulkResult result, int requested) {
        return new BulkOperationResultDto(result.succeeded(), result.failed(),
                requested, result.succeeded().size(), result.failed().size());
    }
}