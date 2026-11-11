package com.team.corporate.backend.dto;

import com.team.corporate.backend.validation.TextValue;
import com.team.corporate.core.entities.Warehouse;

import java.util.UUID;

public record WarehouseDto(UUID id, String name, String location) {
    public static WarehouseDto from(Warehouse warehouse) {
        return new WarehouseDto(warehouse.getId(), warehouse.getName(), warehouse.getLocation());
    }

    public record Request(@TextValue(required = true) String name, @TextValue String location) {
    }
}
