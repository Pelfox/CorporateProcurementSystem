package com.team.corporate.core.services;

import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersService {
    @NotNull
    User createUser(@NotNull String username, @NotNull String passwordHash, @NotNull Instant temporaryPasswordExpiresAt, @NotNull UserRole role);

    @NotNull
    Optional<User> getUser(@NotNull UUID id);

    void deleteUser(@NotNull UUID actingUserId, @NotNull UUID id);

    @NotNull
    User updateUser(@NotNull UUID id, @Nullable String username, @Nullable UserRole role);

    @NotNull
    List<User> getAllByRole(@NotNull UserRole role);

    @NotNull
    List<User> getAll();

    @NotNull
    Optional<User> findByUsername(@NotNull String username);

    /** Legacy lookup only; it does not authenticate a user. */
    @Deprecated
    @NotNull
    default Optional<User> login(@NotNull String username) {
        return findByUsername(username);
    }
}
