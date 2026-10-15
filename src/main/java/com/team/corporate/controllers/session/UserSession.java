package com.team.corporate.controllers.session;

import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;

import java.util.UUID;

public record UserSession(UUID userId, String username, UserRole role) {
    public static UserSession of(User user) {
        return new UserSession(user.getId(), user.getUsername(), user.getUserRole());
    }
}
