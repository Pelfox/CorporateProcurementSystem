package com.team.corporate.backend.security;

import com.team.corporate.core.entities.UserRole;

import java.util.ArrayList;
import java.util.List;

public final class Permissions {
    private Permissions() {
    }

    public static List<String> forRole(UserRole role) {
        var permissions = new ArrayList<>(List.of("catalog:read", "orders:read-own", "orders:create", "orders:edit-own"));
        if (role.canManageProcurement()) {
            permissions.addAll(List.of("catalog:write", "orders:read-all", "orders:status", "orders:delete",
                    "statistics:read", "exports:read"));
        }
        if (role == UserRole.ADMIN) {
            permissions.add("users:admin");
        }
        return List.copyOf(permissions);
    }
}
