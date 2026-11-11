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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class UserManagementServiceImpl implements UserManagementService {
    private final TemporaryPasswordGenerator passwordGenerator;
    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Duration temporaryPasswordTtl;

    public UserManagementServiceImpl(TemporaryPasswordGenerator passwordGenerator, UsersService usersService,
                                     PasswordEncoder passwordEncoder, Clock clock,
                                     @Value("${app.auth.temporary-password-ttl:PT24H}") Duration temporaryPasswordTtl) {
        this.passwordGenerator = passwordGenerator;
        this.usersService = usersService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        if (temporaryPasswordTtl.isNegative() || temporaryPasswordTtl.isZero()) {
            throw new IllegalArgumentException("Срок временного пароля должен быть положительным.");
        }
        this.temporaryPasswordTtl = temporaryPasswordTtl;
    }

    @Override
    @NotNull
    @Transactional
    public CreateUserResponse createUser(@NotNull String username, @NotNull UserRole role) {
        EntityValidation.requireText(username, "Имя пользователя");
        EntityValidation.requireNonNull(role, "Роль");
        String temporaryPassword = passwordGenerator.generate();
        String hashedPassword = passwordEncoder.encode(temporaryPassword);
        Instant expiresAt = clock.instant()
                .plus(temporaryPasswordTtl);

        User user = usersService.createUser(username, hashedPassword, expiresAt, role);
        return new CreateUserResponse(user.getId(), user.getUsername(), user.getUserRole(), temporaryPassword);
    }
}
