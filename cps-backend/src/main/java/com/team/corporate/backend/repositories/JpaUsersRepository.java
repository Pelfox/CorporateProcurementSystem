package com.team.corporate.backend.repositories;

import com.team.corporate.backend.repositories.adapters.UserJpaRepository;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.repositories.UsersRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUsersRepository implements UsersRepository {

    private final UserJpaRepository repository;

    public JpaUsersRepository(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @NotNull
    public User add(@NotNull User user) {
        return repository.save(user);
    }

    @Override
    @NotNull
    public List<User> getAll() {
        return repository.findAll();
    }

    @Override
    @NotNull
    public Optional<User> getById(@NotNull UUID id) {
        return repository.findById(id);
    }

    @Override
    @NotNull
    public User update(@NotNull User user) {
        return repository.save(user);
    }

    @Override
    public void delete(@NotNull UUID id) {
        repository.deleteById(id);
    }
}