package com.team.corporate.services;

import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersService {

    @NotNull User createUser(@NotNull String username, @NotNull UserRole role);

    @NotNull Optional<User> getUser(@NotNull UUID id);

    void deleteUser(@NotNull UUID id);

    @NotNull User updateUser(@NotNull UUID id, String username, UserRole role);

    @NotNull List<User> getAllByRole(@NotNull UserRole role);

    @NotNull List<User> getAll();

    boolean checkIfExists(@NotNull UUID id);


}
