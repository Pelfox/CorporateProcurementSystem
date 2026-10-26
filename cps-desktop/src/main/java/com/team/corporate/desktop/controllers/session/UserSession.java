package com.team.corporate.desktop.controllers.session;

import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;

import java.util.UUID;

public record UserSession(UUID userId, String username, UserRole role) {
    public static UserSession of(User user) {
        return new UserSession(user.getId(), user.getUsername(), user.getUserRole());
    }
}
