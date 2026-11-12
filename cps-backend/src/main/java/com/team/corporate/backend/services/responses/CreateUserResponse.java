package com.team.corporate.backend.services.responses;

import com.team.corporate.core.entities.UserRole;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CreateUserResponse(
        UUID id,
        String username,
        UserRole role,
        String temporaryPassword
) {
    @Override
    @NotNull
    public String toString() {
        return "CreateUserResponse[id=" + id
                + ", temporaryPassword=<redacted>]";
    }
}
