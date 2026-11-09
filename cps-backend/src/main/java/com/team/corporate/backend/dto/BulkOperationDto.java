package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.OrderStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record BulkOperationDto(
        @NotNull BulkAction action,
        @NotEmpty @Size(max = 500) List<UUID> orderIds,
        @Nullable OrderStatus status,
        @Nullable String tagName
) {
    public enum BulkAction {
        ARCHIVE, RESTORE, CHANGE_STATUS, ADD_TAG, REMOVE_TAG
    }
}