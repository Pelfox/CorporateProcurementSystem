package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.AuditLogDto;
import com.team.corporate.backend.dto.OrderDto;
import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.core.entities.Order;
import com.team.corporate.core.entities.OrderItem;
import com.team.corporate.core.services.AuditLogsService;
import com.team.corporate.core.services.OrderItemsService;
import com.team.corporate.core.services.OrdersService;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.*;

import static com.team.corporate.backend.http.QueryExtractor.*;

@RestController
@RequestMapping("/api/orders")
public class OrdersHandler {
    private final OrdersService orders;
    private final OrderItemsService items;
    private final AuditLogsService audit;
    private final ApiExecutor api;

    public OrdersHandler(
            @NotNull OrdersService orders,
            @NotNull OrderItemsService items,
            @NotNull AuditLogsService audit,
            @NotNull ApiExecutor api
    ) {
        this.orders = orders;
        this.items = items;
        this.audit = audit;
        this.api = api;
    }

    @GetMapping
    public List<OrderDto> list(@Valid @ModelAttribute OrderDto.Query query) {
        return list(query, query.userId());
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable UUID id) {
        return details(id);
    }

    @GetMapping("/{id}/items")
    public List<OrderDto.Item> items(@PathVariable UUID id) {
        return details(id).items();
    }

    @GetMapping("/{id}/audit")
    public List<AuditLogDto> audit(@PathVariable UUID id) {
        return api.execute(() -> {
            requireOrder(id);
            return audit.getAllByOrder(id).stream().map(AuditLogDto::from)
                    .sorted(Comparator.comparing(AuditLogDto::changedAt).thenComparing(AuditLogDto::id)).toList();
        });
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(@Valid @RequestBody OrderDto.Request body) {
        UUID id = api.execute(() -> orders.createOrder(body.userId(), body.lines(), body.notes()));
        return ResponseEntity.created(URI.create("/api/orders/" + id)).body(details(id));
    }

    @PutMapping("/{id}")
    public OrderDto update(@PathVariable UUID id, @Valid @RequestBody OrderDto.Request body) {
        api.run(() -> {
            requireOrder(id);
            orders.saveOrder(body.userId(), id, body.lines(), body.notes());
        });
        return details(id);
    }

    @PatchMapping("/{id}/notes")
    public OrderDto notes(@PathVariable UUID id, @Valid @RequestBody OrderDto.NotesRequest body) {
        api.run(() -> {
            requireOrder(id);
            orders.updateNotes(body.userId(), id, body.notes());
        });
        return details(id);
    }

    @PostMapping("/{id}/items")
    public OrderDto addItem(@PathVariable UUID id, @Valid @RequestBody OrderDto.AddItemRequest body) {
        api.run(() -> {
            requireOrder(id);
            orders.addItem(body.userId(), id, body.line());
        });
        return details(id);
    }

    @PutMapping("/{id}/items/{itemId}")
    public OrderDto quantity(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody OrderDto.QuantityRequest body
    ) {
        return changeQuantity(id, itemId, body.quantity(), body.userId());
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public OrderDto deleteItem(@PathVariable UUID id, @PathVariable UUID itemId, @RequestParam UUID userId) {
        return changeQuantity(id, itemId, 0, userId);
    }

    @PatchMapping("/{id}/status")
    public OrderDto status(@PathVariable UUID id, @Valid @RequestBody OrderDto.StatusRequest body) {
        api.run(() -> {
            requireOrder(id);
            orders.changeStatus(body.managerId(), id, body.status());
        });
        return details(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        api.run(() -> {
            requireOrder(id);
            orders.deleteOrder(id);
        });
        return ResponseEntity.noContent().build();
    }

    private OrderDto changeQuantity(@NotNull UUID orderId, @NotNull UUID itemId, int quantity, @NotNull UUID userId) {
        api.run(() -> {
            requireOrder(orderId);
            items.getOrderItem(itemId).filter(item -> item.getOrder().getId().equals(orderId))
                    .orElseThrow(() -> ApiException.notFound("Позиция заказа"));
            orders.updateQuantity(userId, orderId, itemId, quantity);
        });
        return details(orderId);
    }

    private OrderDto details(@NotNull UUID id) {
        return api.execute(() -> OrderDto.from(requireOrder(id), items.getAllByOrder(id)));
    }

    private Order requireOrder(@NotNull UUID id) {
        return orders.getOrder(id)
                .orElseThrow(() -> ApiException.notFound("Заказ"));
    }

    private List<OrderDto> list(@NotNull OrderDto.Query query, @Nullable UUID userId) {
        String q = normalize(query.q());
        Comparator<OrderDto> comparator = switch (query.sort()) {
            case CREATED_ASC -> Comparator.comparing(OrderDto::createdAt);
            case CREATED_DESC -> Comparator.comparing(OrderDto::createdAt).reversed();
            case TOTAL_ASC -> Comparator.comparing(OrderDto::total);
            case TOTAL_DESC -> Comparator.comparing(OrderDto::total).reversed();
        };
        return api.execute(() -> {
            var lines = new HashMap<UUID, List<OrderItem>>();
            for (var item : items.getAll()) {
                lines.computeIfAbsent(item.getOrder().getId(), ignored -> new ArrayList<>()).add(item);
            }
            var source = userId == null ? orders.getAll() : orders.getAllByUser(userId);
            return source.stream().map(order -> OrderDto.from(order, lines.getOrDefault(order.getId(), List.of())))
                    .filter(order -> contains(order.id().toString(), q) || contains(order.notes(), q))
                    .filter(order -> query.status() == null || order.status() == query.status())
                    .filter(order -> query.minTotal() == null || order.total().compareTo(query.minTotal()) >= 0)
                    .filter(order -> query.maxTotal() == null || order.total().compareTo(query.maxTotal()) <= 0)
                    .filter(order -> query.dateFrom() == null
                            || !order.createdAt().atZone(ZoneOffset.UTC).toLocalDate().isBefore(query.dateFrom()))
                    .filter(order -> query.dateTo() == null
                            || !order.createdAt().atZone(ZoneOffset.UTC).toLocalDate().isAfter(query.dateTo()))
                    .sorted(comparator.thenComparing(OrderDto::id)).toList();
        });
    }
}
