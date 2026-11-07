package com.team.corporate.backend.dto;

import com.team.corporate.backend.validation.TextValue;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record UserDto(UUID id, String username, UserRole role, Instant createdAt) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getUserRole(), user.getCreatedAt());
    }

    public enum Sort {
        NAME_ASC,
        NAME_DESC,
        CREATED_ASC,
        CREATED_DESC
    }

    public record Request(@TextValue(required = true) String username, @NotNull UserRole role) {
    }

    public record LookupQuery(@TextValue(required = true) String username) {
    }

    public record Query(@TextValue String q, UserRole role, Sort sort) {
        public Query {
            if (sort == null) {
                sort = Sort.CREATED_DESC;
            }
        }
    }
}
