package com.team.corporate.backend.repositories;

import com.team.corporate.core.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllBySessionId(UUID sessionId);

    @Query("select new com.team.corporate.backend.repositories.SessionReference(t.session.user.id, t.session.id) "
            + "from RefreshToken t where t.tokenHash = :hash")
    Optional<SessionReference> findReferenceByTokenHash(@Param("hash") String hash);

    boolean existsBySessionIdAndExpiresAtAfter(UUID sessionId, Instant expiresAt);

    void deleteAllBySessionId(UUID sessionId);
}
