package com.team.corporate.repositories;

import com.team.corporate.models.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersRepository {
    @NotNull User add(@NotNull User user);

    @NotNull List<User> getAll();

    Optional<User> getById(@NotNull UUID id);

    User update(@NotNull User user);

    void delete(@NotNull UUID id);
}
