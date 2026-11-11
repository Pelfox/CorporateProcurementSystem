package com.team.corporate.backend.repositories.adapters;

import com.team.corporate.core.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
}
