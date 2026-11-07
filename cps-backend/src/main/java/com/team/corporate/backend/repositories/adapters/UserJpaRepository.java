package com.team.corporate.backend.repositories.adapters;

import com.team.corporate.core.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<User, UUID> { }
