package com.team.corporate.domain.repository;


import com.team.corporate.domain.models.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository {
    Warehouse add(Warehouse warehouse);
    List<Warehouse> getAll();
    Optional<Warehouse> getById(int id);
    Warehouse update(Warehouse warehouse);
    void delete(int id);
}
