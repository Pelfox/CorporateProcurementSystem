package com.team.corporate.core.entities;

public enum UserRole {
    USER,
    MANAGER,
    ADMIN;

    public boolean canManageProcurement() {
        return this == MANAGER || this == ADMIN;
    }
}
