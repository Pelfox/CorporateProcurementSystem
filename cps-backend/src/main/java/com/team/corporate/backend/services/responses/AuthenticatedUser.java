package com.team.corporate.backend.services.responses;

import com.team.corporate.core.entities.UserRole;

import java.time.Instant;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String username, UserRole role,
                                boolean mustChangePassword, Instant temporaryPasswordExpiresAt) {
}
