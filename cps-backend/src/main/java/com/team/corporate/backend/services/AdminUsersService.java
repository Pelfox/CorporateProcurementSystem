package com.team.corporate.backend.services;

import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.repositories.AuthSessionRepository;
import com.team.corporate.backend.repositories.RefreshTokenRepository;
import com.team.corporate.backend.repositories.adapters.UserJpaRepository;
import com.team.corporate.backend.security.BusinessAccess;
import com.team.corporate.backend.security.SecurityAudit;
import com.team.corporate.backend.security.SessionValidator;
import com.team.corporate.backend.security.TemporaryPasswordGenerator;
import com.team.corporate.backend.services.responses.CreateUserResponse;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@PreAuthorize("hasAuthority('SESSION_FULL') and hasRole('ADMIN')")
public class AdminUsersService {
    private final UserJpaRepository users;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository tokens;
    private final UserManagementService management;
    private final BusinessAccess access;
    private final SessionValidator validator;
    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final TemporaryPasswordGenerator generator;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final Duration temporaryTtl;

    public AdminUsersService(UserJpaRepository users, AuthSessionRepository sessions, RefreshTokenRepository tokens,
                             UserManagementService management, BusinessAccess access, SessionValidator validator,
                             EntityManager em, JdbcTemplate jdbc, TemporaryPasswordGenerator generator,
                             PasswordEncoder encoder, Clock clock,
                             @Value("${app.auth.temporary-password-ttl:PT24H}") Duration temporaryTtl) {
        this.users = users;
        this.sessions = sessions;
        this.tokens = tokens;
        this.management = management;
        this.access = access;
        this.validator = validator;
        this.em = em;
        this.jdbc = jdbc;
        this.generator = generator;
        this.encoder = encoder;
        this.clock = clock;
        this.temporaryTtl = temporaryTtl;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return users.findAll().stream().sorted(Comparator.comparing(User::getUsername)).toList();
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Пользователь"));
    }

    @Transactional
    public CreateUserResponse create(String username, UserRole role) {
        prepare(null, "USER_CREATE");
        var result = management.createUser(username, role);
        SecurityAudit.success("USER_CREATE", access.principal().userId(), result.id());
        return result;
    }

    @Transactional
    public User enabled(UUID id, boolean enabled) {
        var user = prepare(id, enabled ? "USER_ENABLE" : "USER_DISABLE");
        if (!enabled) {
            protectLastAdmin(user);
            revoke(user);
        }
        user.setEnabled(enabled);
        SecurityAudit.success(enabled ? "USER_ENABLE" : "USER_DISABLE", access.principal().userId(), id);
        return user;
    }

    @Transactional
    public User role(UUID id, UserRole role) {
        var user = prepare(id, "USER_ROLE");
        if (role != UserRole.ADMIN) {
            protectLastAdmin(user);
        }
        user.setUserRole(role);
        SecurityAudit.success("USER_ROLE", access.principal().userId(), id);
        return user;
    }

    @Transactional
    public CreateUserResponse reset(UUID id) {
        var user = prepare(id, "USER_RESET");
        String temporary = generator.generate();
        user.setTemporaryPassword(encoder.encode(temporary), clock.instant().plus(temporaryTtl));
        revoke(user);
        SecurityAudit.success("USER_RESET", access.principal().userId(), id);
        return new CreateUserResponse(id, user.getUsername(), user.getUserRole(), temporary);
    }

    @Transactional
    public void delete(UUID id) {
        var user = prepare(id, "USER_DELETE");
        protectLastAdmin(user);
        long references = jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM orders WHERE user_id = ?) + "
                + "(SELECT COUNT(*) FROM audit_logs WHERE changed_by = ?)", Long.class, id, id);
        if (references > 0) {
            throw conflict("Пользователь связан с заказами или аудитом. Используйте блокировку.");
        }
        for (var session : sessions.findAllByUserId(id)) {
            tokens.deleteAllBySessionId(session.getId());
            tokens.flush();
            sessions.delete(session);
        }
        sessions.flush();
        users.delete(user);
        users.flush();
        SecurityAudit.success("USER_DELETE", access.principal().userId(), id);
    }

    /** Lock order: membership guard -> users in UUID order -> their sessions/tokens. */
    private User prepare(UUID target, String action) {
        SecurityAudit.failureOnRollback(action, access.principal().userId(), target);
        jdbc.queryForObject("SELECT id FROM security_admin_guard WHERE id = 1 FOR UPDATE", Integer.class);
        var principal = access.principal();
        for (var id : Stream.of(principal.userId(), target).filter(java.util.Objects::nonNull).distinct().sorted().toList()) {
            var user = users.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Пользователь"));
            em.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        }
        var actor = users.findById(principal.userId()).orElseThrow();
        var session = sessions.findById(principal.sessionId()).orElseThrow(() -> new AccessDeniedException("Сессия отозвана."));
        em.refresh(session);
        if (actor.getUserRole() != UserRole.ADMIN || !session.getUser().getId().equals(actor.getId())
                || !validator.isActive(session, actor)) {
            throw new AccessDeniedException("Требуются актуальные права ADMIN.");
        }
        return target == null ? null : users.findById(target).orElseThrow();
    }

    private void protectLastAdmin(User user) {
        if (user.isEnabled() && user.getUserRole() == UserRole.ADMIN
                && users.countByUserRoleAndEnabledTrue(UserRole.ADMIN) <= 1) {
            throw conflict("Нельзя отключить, удалить или понизить последнего активного ADMIN.");
        }
    }

    private void revoke(User user) {
        for (var session : sessions.findAllByUserId(user.getId())) {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(clock.instant());
            }
        }
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
