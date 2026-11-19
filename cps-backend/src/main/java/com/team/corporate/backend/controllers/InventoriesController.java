package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateInventoryRequest;
import com.team.corporate.backend.dto.InventoryResponse;
import com.team.corporate.backend.dto.UpdateInventoryRequest;
import com.team.corporate.backend.search.InventoryFilter;
import com.team.corporate.backend.services.InventoriesSearchService;
import com.team.corporate.core.exceptions.InventoryNotFoundException;
import com.team.corporate.core.services.InventoriesService;
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
@RequestMapping("/api/inventories")
public class InventoriesController {
    private final InventoriesService inventories;
    private final InventoriesSearchService search;

    public InventoriesController(InventoriesService inventories, InventoriesSearchService search) {
        this.inventories = inventories;
        this.search = search;
    }

    @GetMapping
    public PagedModel<InventoryResponse> getInventories(
            @Valid @ModelAttribute InventoryFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Sort sort
    ) {
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @GetMapping("/{id}")
    public InventoryResponse getInventory(@PathVariable("id") UUID id) {
        return inventories.getInventory(id)
                .map(InventoryResponse::of)
                .orElseThrow(() -> new InventoryNotFoundException("Складской остаток не найден."));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        var inventory = inventories.placeProductInWarehouse(
                request.productId(), request.warehouseId(), request.quantity());
        return ResponseEntity.created(URI.create("/api/inventories/" + inventory.getId()))
                .body(InventoryResponse.of(inventory));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public InventoryResponse updateInventory(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateInventoryRequest request
    ) {
        return InventoryResponse.of(inventories.updateInventory(id, request.quantity()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventory(@PathVariable("id") UUID id) {
        inventories.deleteInventory(id);
    }
}
