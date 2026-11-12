package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.AuthSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    List<AuthSession> findAllByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSession s where s.id = :id")
    Optional<AuthSession> findByIdForUpdate(@Param("id") UUID id);

    @Query("select new com.team.corporate.backend.repositories.SessionReference(s.user.id, s.id) "
            + "from AuthSession s where s.expiresAt <= :before order by s.user.id, s.id")
    List<SessionReference> findExpiredReferences(@Param("before") Instant before, Pageable page);
}
