package com.team.corporate.backend.services;

import com.team.corporate.backend.repositories.adapters.UserJpaRepository;
import com.team.corporate.backend.security.SecurityAudit;
import com.team.corporate.backend.validation.PasswordPolicy;
import com.team.corporate.backend.validation.UsernamePolicy;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/** Explicit offline provisioning exception; the secret is supplied by the operator, never a default. */
@Service
public class BootstrapAdminService {
    private final UserJpaRepository users;
    private final JdbcTemplate jdbc;
    private final PasswordPolicy passwords;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final Duration ttl;

    public BootstrapAdminService(UserJpaRepository users, JdbcTemplate jdbc, PasswordPolicy passwords,
                                 PasswordEncoder encoder, Clock clock,
                                 @Value("${app.auth.temporary-password-ttl:PT24H}") Duration ttl) {
        this.users = users;
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.encoder = encoder;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Transactional
    public void provision(String username, java.util.function.Supplier<String> secret) {
        jdbc.queryForObject("SELECT id FROM security_admin_guard WHERE id = 1 FOR UPDATE", Integer.class);
        if (users.existsByUserRole(UserRole.ADMIN)) {
            return;
        }
        String normalized = UsernamePolicy.normalize(username);
        String temporary = secret.get();
        passwords.validateNewPassword(temporary);
        if (users.findByUsername(normalized).isPresent()) {
            throw new IllegalStateException("Bootstrap login занят. Выберите отдельную учётную запись.");
        }
        var user = users.saveAndFlush(new User(normalized, encoder.encode(temporary), clock.instant().plus(ttl), UserRole.ADMIN));
        SecurityAudit.success("ADMIN_BOOTSTRAP", null, user.getId());
    }
}
