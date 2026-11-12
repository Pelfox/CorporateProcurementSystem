package com.team.corporate.backend.repositories;

import java.util.UUID;

/** Projection avoids caching token/user state before the transaction obtains its locks. */
public record SessionReference(UUID userId, UUID sessionId) {
}
