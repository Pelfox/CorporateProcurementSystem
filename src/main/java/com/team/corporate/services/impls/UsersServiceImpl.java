package com.team.corporate.services.impls;

import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;
import com.team.corporate.exceptions.UserNotFoundException;
import com.team.corporate.repositories.UsersRepository;
import com.team.corporate.services.UsersService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UsersServiceImpl implements UsersService {
    private final UsersRepository usersRepository;

    public UsersServiceImpl(@NotNull UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public @NotNull User createUser(@NotNull String username, @NotNull UserRole role) {
        User newUser = new User(username, role);
        return usersRepository.add(newUser);
    }

    @Override
    public @NotNull Optional<User> getUser(@NotNull UUID id) {
        return usersRepository.getById(id);
    }

    @Override
    public void deleteUser(@NotNull UUID id) {
        usersRepository.delete(id);
    }

    @Override
    public @NotNull User updateUser(@NotNull UUID id, @Nullable String username, @Nullable UserRole role) {
        User user = usersRepository.getById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с указанным ID не найден"));
        if (username != null && !username.equals(user.getUsername())) {
            user.setUsername(username);
        }
        if (role != null) {
            user.setUserRole(role);
        }
        return usersRepository.update(user);
    }

    @Override
    public @NotNull List<User> getAllByRole(@NotNull UserRole role) {
        return usersRepository.getAll()
                .stream()
                .filter(user -> user.getUserRole().equals(role))
                .toList();
    }

    @Override
    public @NotNull List<User> getAll() {
        return usersRepository.getAll();
    }

    @Override
    public @NotNull Optional<User> login(@NotNull String username) {
        return Optional.empty();
    }
}
