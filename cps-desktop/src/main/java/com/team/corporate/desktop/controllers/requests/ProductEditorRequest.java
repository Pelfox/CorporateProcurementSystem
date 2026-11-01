package com.team.corporate.desktop.controllers.requests;

import com.team.corporate.core.entities.Product;

import java.util.UUID;

public record ProductEditorRequest(UUID productId) {
    public static ProductEditorRequest forCreate() {
        return new ProductEditorRequest(null);
    }

    public static ProductEditorRequest forEdit(Product product) {
        return new ProductEditorRequest(product.getId());
    }

    public boolean isEditing() {
        return productId != null;
    }
}
