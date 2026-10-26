package com.team.corporate.core.repositories;

import com.team.corporate.core.entities.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersRepository {
    @NotNull
    User add(@NotNull User user);

    @NotNull
    List<User> getAll();

    @NotNull
    Optional<User> getById(@NotNull UUID id);

    @NotNull
    User update(@NotNull User user);

    void delete(@NotNull UUID id);
}
