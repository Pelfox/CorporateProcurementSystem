package com.team.corporate.services.impls;

import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;
import com.team.corporate.exceptions.UserNotFoundException;
import com.team.corporate.repositories.UsersRepository;
import com.team.corporate.services.UsersService;
import com.team.corporate.utils.EntityValidation;
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
    @NotNull
    public User createUser(@NotNull String username, @NotNull UserRole role) {
        EntityValidation.requireNonNull(username, "Имя пользователя");
        EntityValidation.requireNonNull(role, "Роль");
        User newUser = new User(username, role);
        requireAvailableUsername(newUser.getUsername(), null);
        return usersRepository.add(newUser);
    }

    @Override
    @NotNull
    public Optional<User> getUser(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        return usersRepository.getById(id);
    }

    @Override
    public void deleteUser(@NotNull UUID id) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        usersRepository.delete(id);
    }

    @Override
    @NotNull
    public User updateUser(@NotNull UUID id, @Nullable String username, @Nullable UserRole role) {
        EntityValidation.requireNonNull(id, "Идентификатор");
        User user = usersRepository.getById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с указанным идентификатором не найден"));
        if (username != null && !username.equals(user.getUsername())) {
            requireAvailableUsername(EntityValidation.requireText(username, "Имя пользователя"), id);
            user.setUsername(username);
        }
        if (role != null) {
            user.setUserRole(role);
        }
        return usersRepository.update(user);
    }

    @Override
    @NotNull
    public List<User> getAllByRole(@NotNull UserRole role) {
        EntityValidation.requireNonNull(role, "Роль");
        return usersRepository.getAll()
                .stream()
                .filter(user -> user.getUserRole().equals(role))
                .toList();
    }

    @Override
    @NotNull
    public List<User> getAll() {
        return usersRepository.getAll();
    }

    @Override
    @NotNull
    public Optional<User> login(@NotNull String username) {
        EntityValidation.requireNonNull(username, "Имя пользователя");
        String normalized = EntityValidation.requireText(username, "Имя пользователя");
        return usersRepository.getAll().stream()
                .filter(user -> user.getUsername().equals(normalized))
                .findFirst();
    }

    private void requireAvailableUsername(String username, UUID currentId) {
        if (usersRepository.getAll().stream().anyMatch(existing ->
                existing.getUsername().equals(username) && !existing.getId().equals(currentId))) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует.");
        }
    }
}
