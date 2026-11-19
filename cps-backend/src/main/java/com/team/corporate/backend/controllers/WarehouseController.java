package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateWarehouseRequest;
import com.team.corporate.backend.dto.UpdateWarehouseRequest;
import com.team.corporate.backend.dto.WarehouseResponse;
import com.team.corporate.backend.search.WarehouseFilter;
import com.team.corporate.backend.services.WarehousesSearchService;
import com.team.corporate.core.exceptions.WarehouseNotFoundException;
import com.team.corporate.core.services.WarehousesService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {
    private final WarehousesService warehouses;
    private final WarehousesSearchService search;

    public WarehouseController(WarehousesService warehouses, WarehousesSearchService search) {
        this.warehouses = warehouses;
        this.search = search;
    }

    @GetMapping
    public PagedModel<WarehouseResponse> getWarehouses(
            @Valid @ModelAttribute WarehouseFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Sort sort
    ) {
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @GetMapping("/{id}")
    public WarehouseResponse getWarehouse(@PathVariable("id") UUID id) {
        return warehouses.getWarehouse(id)
                .map(WarehouseResponse::of)
                .orElseThrow(() -> new WarehouseNotFoundException("Склад не найден."));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request
    ) {
        var warehouse = warehouses.createWarehouse(request.name(), request.location());
        return ResponseEntity.created(URI.create("/api/warehouses/" + warehouse.getId()))
                .body(WarehouseResponse.of(warehouse));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public WarehouseResponse updateWarehouse(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateWarehouseRequest request
    ) {
        return WarehouseResponse.of(warehouses.updateWarehouse(id, request.name(), request.location()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWarehouse(@PathVariable("id") UUID id) {
        warehouses.deleteWarehouse(id);
    }
}
