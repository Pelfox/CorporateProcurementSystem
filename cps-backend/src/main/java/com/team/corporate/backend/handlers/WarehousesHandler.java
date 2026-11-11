package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.SearchQuery;
import com.team.corporate.backend.dto.WarehouseDto;
import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.core.services.WarehousesService;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.team.corporate.backend.http.QueryExtractor.*;

@RestController
@RequestMapping("/api/warehouses")
public class WarehousesHandler {
    private final WarehousesService warehouses;
    private final ApiExecutor api;

    public WarehousesHandler(@NotNull WarehousesService warehouses, @NotNull ApiExecutor api) {
        this.warehouses = warehouses;
        this.api = api;
    }

    @GetMapping
    public List<WarehouseDto> list(@Valid @ModelAttribute SearchQuery query) {
        String q = normalize(query.q());
        return api.execute(() -> warehouses.getAll().stream().map(WarehouseDto::from)
                .filter(warehouse -> contains(warehouse.name(), q) || contains(warehouse.location(), q))
                .sorted(Comparator.comparing(WarehouseDto::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(WarehouseDto::id)).toList());
    }

    @GetMapping("/{id}")
    public WarehouseDto get(@PathVariable UUID id) {
        return api.execute(() -> WarehouseDto.from(warehouses.getWarehouse(id)
                .orElseThrow(() -> ApiException.notFound("Склад"))));
    }

    @PostMapping
    public ResponseEntity<WarehouseDto> create(@Valid @RequestBody WarehouseDto.Request body) {
        var result = api.execute(() -> WarehouseDto.from(warehouses.createWarehouse(body.name(), body.location())));
        return ResponseEntity.created(URI.create("/api/warehouses/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    public WarehouseDto update(@PathVariable UUID id, @Valid @RequestBody WarehouseDto.Request body) {
        return api.execute(() -> WarehouseDto.from(warehouses.saveWarehouse(id, body.name(), body.location())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        api.run(() -> {
            warehouses.getWarehouse(id).orElseThrow(() -> ApiException.notFound("Склад"));
            warehouses.deleteWarehouse(id);
        });
        return ResponseEntity.noContent().build();
    }
}
