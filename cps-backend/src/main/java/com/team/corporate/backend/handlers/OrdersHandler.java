package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.AuditLogDto;
import com.team.corporate.backend.dto.OrderDto;
import com.team.corporate.backend.filter.PageQuery;
import com.team.corporate.backend.filter.PageResponse;
import com.team.corporate.backend.filter.Paginator;
import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.backend.security.BusinessAccess;
import com.team.corporate.core.entities.Order;
import com.team.corporate.core.services.AuditLogsService;
import com.team.corporate.core.services.OrderItemsService;
import com.team.corporate.core.services.OrdersService;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.team.corporate.backend.http.QueryExtractor.contains;
import static com.team.corporate.backend.http.QueryExtractor.normalize;

@RestController
@RequestMapping("/api/orders")
public class OrdersHandler {
    private final OrdersService orders;
    private final OrderItemsService items;
    private final AuditLogsService audit;
    private final ApiExecutor api;
    private final BusinessAccess access;

    public OrdersHandler(
            @NotNull OrdersService orders,
            @NotNull OrderItemsService items,
            @NotNull AuditLogsService audit,
            @NotNull ApiExecutor api,
            @NotNull BusinessAccess access
    ) {
        this.orders = orders;
        this.items = items;
        this.audit = audit;
        this.api = api;
        this.access = access;
    }

    @GetMapping
    public List<OrderDto> list(@Valid @ModelAttribute OrderDto.Query query) {
        return list(query, access.orderScope(query.userId()));
    }

    @GetMapping("/page")
    public PageResponse<OrderDto> page(
            @Valid @ModelAttribute OrderDto.Query query,
            @Valid @ModelAttribute PageQuery pagination
    ) {
        return Paginator.paginate(list(query), pagination);
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

    @GetMapping("/{id}/items/page")
    public PageResponse<OrderDto.Item> itemsPage(
            @PathVariable UUID id,
            @Valid @ModelAttribute PageQuery pagination
    ) {
        return Paginator.paginate(items(id), pagination);
    }

    @GetMapping("/{id}/audit/page")
    public PageResponse<AuditLogDto> auditPage(
            @PathVariable UUID id,
            @Valid @ModelAttribute PageQuery pagination
    ) {
        return Paginator.paginate(audit(id), pagination);
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(@Valid @RequestBody OrderDto.Request body) {
        UUID id = api.execute(() -> orders.createOrder(access.actor(body.userId()), body.lines(), body.notes()));
        return ResponseEntity.created(URI.create("/api/orders/" + id)).body(details(id));
    }

    @PutMapping("/{id}")
    public OrderDto update(@PathVariable UUID id, @Valid @RequestBody OrderDto.Request body) {
        api.run(() -> {
            requireOrder(id);
            orders.saveOrder(access.actor(body.userId()), id, body.lines(), body.notes());
        });
        return details(id);
    }

    @PatchMapping("/{id}/notes")
    public OrderDto notes(@PathVariable UUID id, @Valid @RequestBody OrderDto.NotesRequest body) {
        api.run(() -> {
            requireOrder(id);
            orders.updateNotes(access.actor(body.userId()), id, body.notes());
        });
        return details(id);
    }

    @PostMapping("/{id}/items")
    public OrderDto addItem(@PathVariable UUID id, @Valid @RequestBody OrderDto.AddItemRequest body) {
        api.run(() -> {
            requireOrder(id);
            orders.addItem(access.actor(body.userId()), id, body.line());
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
            orders.changeStatus(access.actor(body.managerId()), id, body.status());
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
            orders.updateQuantity(access.actor(userId), orderId, itemId, quantity);
        });
        return details(orderId);
    }

    private OrderDto details(@NotNull UUID id) {
        return api.execute(() -> OrderDto.from(requireOrder(id), items.getAllByOrder(id)));
    }

    private Order requireOrder(@NotNull UUID id) {
        return access.requireOrder(id);
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
            var source = userId == null ? orders.getAll() : orders.getAllByUser(userId);
            return source.stream().map(order -> OrderDto.from(order, items.getAllByOrder(order.getId())))
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
