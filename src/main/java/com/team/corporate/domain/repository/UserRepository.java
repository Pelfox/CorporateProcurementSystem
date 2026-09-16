package com.team.corporate.domain.repository;

import com.team.corporate.domain.models.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User add(User user);
    List<User> getAll();
    Optional<User> getById(UUID id);
    User update(User user);
    void delete(UUID id);
}
