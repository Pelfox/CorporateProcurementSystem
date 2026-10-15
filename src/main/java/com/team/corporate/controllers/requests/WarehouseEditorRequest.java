package com.team.corporate.controllers.requests;

import com.team.corporate.entities.Warehouse;

import java.util.UUID;

public record WarehouseEditorRequest(UUID warehouseId) {
    public static WarehouseEditorRequest forCreate() { return new WarehouseEditorRequest(null); }

    public static WarehouseEditorRequest forEdit(Warehouse warehouse) { return new WarehouseEditorRequest(warehouse.getId()); }

    public boolean isEditing() { return warehouseId != null; }
}
