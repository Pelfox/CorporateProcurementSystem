package com.team.corporate.repositories;

import com.team.corporate.entities.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersRepository {
    /**
     * Persists a new user and returns it with its generated ID.
     */
    @NotNull User add(@NotNull User user);

    @NotNull List<User> getAll();

    /**
     * Returns an empty optional when no user exists with the given ID.
     */
    @NotNull Optional<User> getById(@NotNull UUID id);

    /**
     * Merges an existing user's changes and returns the saved copy.
     *
     * @throws IllegalArgumentException if the user has no ID
     */
    @NotNull User update(@NotNull User user);

    /**
     * Deletes the user if present; does nothing when no user exists with the given ID.
     */
    void delete(@NotNull UUID id);
}
