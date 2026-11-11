package com.team.corporate.backend.security;

import com.team.corporate.core.entities.SessionType;
import com.team.corporate.core.entities.UserRole;

import java.security.Principal;
import java.util.UUID;

public record SessionPrincipal(UUID userId, UUID sessionId, String username, UserRole role, SessionType type)
        implements Principal {
    @Override
    public String getName() {
        return username;
    }
}
