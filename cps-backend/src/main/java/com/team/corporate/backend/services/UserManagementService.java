package com.team.corporate.backend.services;

import com.team.corporate.backend.services.responses.CreateUserResponse;
import com.team.corporate.core.entities.UserRole;
import org.jetbrains.annotations.NotNull;

public interface UserManagementService {
    @NotNull
    CreateUserResponse createUser(@NotNull String username, @NotNull UserRole role);
}
