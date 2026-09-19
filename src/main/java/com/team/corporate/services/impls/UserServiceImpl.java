package com.team.corporate.services.impls;

import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;
import com.team.corporate.repositories.UsersRepository;
import com.team.corporate.services.UsersService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class UserServiceImpl implements UsersService {

    public UsersRepository usersRepository;

    public UserServiceImpl(@NotNull UsersRepository usersRepository) {
        this.usersRepository = Objects.requireNonNull(usersRepository, "userRepository must not be none.");
    }

    @Override
    public @NotNull User createUser(@NotNull String username, @NotNull UserRole role) {
        return null;
    }

    @Override
    public @NotNull Optional<User> getUser(@NotNull UUID id) {
        return null;
    }

    @Override
    public void deleteUser(@NotNull UUID id) {

    }

    @Override
    public @NotNull User updateUser(@NotNull UUID id, @NotNull String username, @NotNull UserRole role) {
        return null;
    }

    @Override
    public @NotNull List<User> getAllByRole(@NotNull UserRole role) {
        return List.of();
    }

    @Override
    public @NotNull List<User> getAll() {
        return List.of();
    }

    @Override
    public @NotNull Optional<User> login(@NotNull String username) {
        return Optional.empty();
    }

}
