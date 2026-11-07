package com.team.corporate.backend.services.impls;

import com.team.corporate.backend.security.TemporaryPasswordGenerator;
import com.team.corporate.backend.services.UserManagementService;
import com.team.corporate.backend.services.responses.CreateUserResponse;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.services.UsersService;
import com.team.corporate.core.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class UserManagementServiceImpl implements UserManagementService {
    private final TemporaryPasswordGenerator passwordGenerator;
    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserManagementServiceImpl(TemporaryPasswordGenerator passwordGenerator, UsersService usersService, PasswordEncoder passwordEncoder, Clock clock) {
        this.passwordGenerator = passwordGenerator;
        this.usersService = usersService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @NotNull
    public CreateUserResponse createUser(@NotNull String username, @NotNull UserRole role) {
        EntityValidation.requireText(username, "Имя пользователя");
        EntityValidation.requireNonNull(role, "Роль");
        String temporaryPassword = passwordGenerator.generate();
        String hashedPassword = passwordEncoder.encode(temporaryPassword);
        Instant expiresAt = clock.instant()
                .plus(Duration.ofHours(24));

        User user = usersService.createUser(username, hashedPassword, expiresAt, role);
        return new CreateUserResponse(user.getId(), user.getUsername(), user.getUserRole(), temporaryPassword);
    }
}
