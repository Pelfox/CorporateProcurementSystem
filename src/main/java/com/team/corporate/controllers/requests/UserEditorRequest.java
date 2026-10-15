package com.team.corporate.controllers.requests;

import com.team.corporate.entities.User;

import java.util.UUID;

public record UserEditorRequest(UUID userId) {
    public static UserEditorRequest forCreate() { return new UserEditorRequest(null); }

    public static UserEditorRequest forEdit(User user) { return new UserEditorRequest(user.getId()); }

    public boolean isEditing() { return userId != null; }
}
