package com.team.corporate.controllers.requests;

import com.team.corporate.entities.Category;

import java.util.UUID;

public record CategoryEditorRequest(UUID categoryId) {
    public static CategoryEditorRequest forCreate() { return new CategoryEditorRequest(null); }

    public static CategoryEditorRequest forEdit(Category category) { return new CategoryEditorRequest(category.getId()); }

    public boolean isEditing() { return categoryId != null; }
}
