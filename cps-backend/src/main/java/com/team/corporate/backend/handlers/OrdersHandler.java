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
import com.team.corporate.core.services.TagsService;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.team.corporate.backend.http.QueryExtractor.*;

@RestController
@RequestMapping("/api/orders")
public class OrdersHandler {
    private final OrdersService orders;
    private final OrderItemsService items;
    private final TagsService tagsService;
    private final AuditLogsService auditLogsService;
    private final ApiExecutor api;

    public OrdersHandler(
            @NotNull OrdersService orders,
            @NotNull OrderItemsService items,
            @NotNull TagsService tagsService,
            @NotNull AuditLogsService auditLogsService,
            @NotNull ApiExecutor api
    ) {
        this.orders = orders;
        this.items = items;
        this.tagsService = tagsService;
        this.auditLogsService = auditLogsService;
        this.api = api;
    }

    @GetMapping
    public List<OrderDto> list(@Valid @ModelAttribute OrderDto.Query query) {
        String q = normalize(query.q());
        Comparator<OrderDto> comparator = switch (query.sort()) {
            case CREATED_ASC -> Comparator.comparing(OrderDto::createdAt);
            case CREATED_DESC -> Comparator.comparing(OrderDto::createdAt).reversed();
            case TOTAL_ASC -> Comparator.comparing(OrderDto::total);
            case TOTAL_DESC -> Comparator.comparing(OrderDto::total).reversed();
        };
        return api.execute(() -> {
            var source = query.userId() == null
                    ? orders.getAll()
                    : orders.getAllByUser(query.userId());
            return source.stream()
                    .filter(order -> query.status() == null || order.getStatus() == query.status())
                    .filter(order -> query.archived() == null || order.isArchived() == query.archived())
                    .filter(order -> query.tag() == null || tagsService.getByOrder(order.getId()).stream()
                            .anyMatch(t -> t.getName().equalsIgnoreCase(query.tag())))
                    .map(order -> OrderDto.from(order, items.getAllByOrder(order.getId()),
                            tagsService.getByOrder(order.getId())))
                    .filter(dto -> contains(dto.username(), q) || contains(dto.id().toString(), q)
                            || contains(dto.notes(), q) || (dto.externalCode() != null && contains(dto.externalCode(), q)))
                    .filter(dto -> query.minTotal() == null || dto.total().compareTo(query.minTotal()) >= 0)
                    .filter(dto -> query.maxTotal() == null || dto.total().compareTo(query.maxTotal()) <= 0)
                    .filter(dto -> query.dateFrom() == null
                            || !dto.createdAt().isBefore(query.dateFrom().atStartOfDay().toInstant(ZoneOffset.UTC)))
                    .filter(dto -> query.dateTo() == null
                            || !dto.createdAt().isAfter(query.dateTo().atTime(23, 59, 59).toInstant(ZoneOffset.UTC)))
                    .sorted(comparator.thenComparing(OrderDto::id)).toList();
        });
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable UUID id) {
        return find(id);
    }

    @GetMapping("/{id}/items")
    public List<OrderDto.Item> items(@PathVariable UUID id) {
        return api.execute(() -> {
            orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
            return items.getAllByOrder(id).stream()
                    .sorted(Comparator.comparing(OrderItem::getId))
                    .map(OrderDto.Item::from).toList();
        });
    }

    @GetMapping("/{id}/audit")
    public List<AuditLogDto> audit(@PathVariable UUID id) {
        return api.execute(() -> {
            orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
            return auditLogsService.getAllByOrder(id).stream()
                    .map(AuditLogDto::from).toList();
        });
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(@Valid @RequestBody OrderDto.Request body) {
        var result = api.execute(() -> {
            var orderId = orders.createOrder(body.userId(), body.lines(), body.notes());
            var order = orders.getOrder(orderId).orElseThrow();
            return OrderDto.from(order, items.getAllByOrder(orderId), tagsService.getByOrder(orderId));
        });
        return ResponseEntity.created(URI.create("/api/orders/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    public OrderDto update(@PathVariable UUID id, @Valid @RequestBody OrderDto.Request body) {
        api.run(() -> {
            orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
            orders.saveOrder(body.userId(), id, body.lines(), body.notes());
        });
        return find(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        api.run(() -> {
            orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
            orders.deleteOrder(id);
        });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    public OrderDto addItem(@PathVariable UUID id, @Valid @RequestBody OrderDto.AddItemRequest body) {
        api.run(() -> {
            orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
            orders.addItem(body.userId(), id, body.line());
        });
        return find(id);
    }

    @PutMapping("/{id}/items/{itemId}")
    public OrderDto updateItem(@PathVariable UUID id, @PathVariable UUID itemId,
                               @Valid @RequestBody OrderDto.QuantityRequest body) {
        api.run(() -> {
            orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
            orders.updateQuantity(body.userId(), id, itemId, body.quantity());
        });
        return find(id);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public OrderDto removeItem(@PathVariable UUID id, @PathVariable UUID itemId,
                               @RequestParam UUID userId) {
        api.run(() -> {
            orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
            orders.updateQuantity(userId, id, itemId, 0);
        });
        return find(id);
    }

    @PatchMapping("/{id}/notes")
    public OrderDto updateNotes(@PathVariable UUID id, @Valid @RequestBody OrderDto.NotesRequest body) {
        api.run(() -> {
            orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
            orders.updateNotes(body.userId(), id, body.notes());
        });
        return find(id);
    }

    @PatchMapping("/{id}/status")
    public OrderDto changeStatus(@PathVariable UUID id, @Valid @RequestBody OrderDto.StatusRequest body) {
        api.run(() -> {
            orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
            orders.changeStatus(body.managerId(), id, body.status());
        });
        return find(id);
    }

    private OrderDto find(UUID id) {
        return api.execute(() -> {
            Order order = orders.getOrder(id)
                    .orElseThrow(() -> ApiException.notFound("Заказ"));
            return OrderDto.from(order, items.getAllByOrder(id), tagsService.getByOrder(id));
        });
    }
}