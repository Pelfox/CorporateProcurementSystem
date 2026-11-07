package com.team.corporate.backend.dto;

import com.team.corporate.backend.validation.TextValue;
import com.team.corporate.core.entities.Category;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CategoryDto(
        UUID id,
        String name,
        long productCount
) {
    public static CategoryDto from(@NotNull Category category, long productCount) {
        return new CategoryDto(category.getId(), category.getName(), productCount);
    }

    public record Request(@TextValue(required = true) String name) {
    }
}
