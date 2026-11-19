package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateOrderItemRequest;
import com.team.corporate.backend.dto.CreateOrderRequest;
import com.team.corporate.backend.dto.OrderItemResponse;
import com.team.corporate.backend.dto.OrderResponse;
import com.team.corporate.backend.dto.UpdateOrderItemRequest;
import com.team.corporate.backend.dto.UpdateOrderRequest;
import com.team.corporate.backend.search.OrderFilter;
import com.team.corporate.backend.services.OrdersSearchService;
import com.team.corporate.core.entities.OrderItem;
import com.team.corporate.core.exceptions.OrderItemNotFoundException;
import com.team.corporate.core.services.OrderItemsService;
import com.team.corporate.core.services.OrdersService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {
    private final OrdersService orders;
    private final OrdersSearchService search;
    private final OrderItemsService items;

    public OrdersController(OrdersService orders, OrdersSearchService search, OrderItemsService items) {
        this.orders = orders;
        this.search = search;
        this.items = items;
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(
            @PathVariable("id") UUID id
    ) {
        return search.getById(id);
    }
    @GetMapping
    public PagedModel<OrderResponse> getOrders(
            @Valid @ModelAttribute OrderFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Sort sort
    ) {
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        var order = orders.createOrder(request.userId(), request.status(), request.notes());
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
                .body(search.getById(order.getId()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("#request.status() == null or hasAnyRole('MANAGER', 'ADMIN')")
    @Transactional
    public OrderResponse updateOrder(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (request.status() != null) {
            orders.changeStatus(UUID.fromString(jwt.getSubject()), id, request.status());
        }
        if (request.notes() != null) {
            orders.updateOrder(id, null, request.notes());
        }
        return search.getById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable("id") UUID id) {
        orders.deleteOrder(id);
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderItemResponse> addProductToOrder(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody CreateOrderItemRequest request
    ) {
        var item = items.createOrderItemForOrder(
                orderId, request.productId(), request.quantity(), request.purchasePrice());
        return ResponseEntity.created(URI.create("/api/orders/" + orderId + "/items/" + item.getId()))
                .body(OrderItemResponse.of(item));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrderItem(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("itemId") UUID itemId
    ) {
        requireOrderItem(orderId, itemId);
        items.deleteOrderItem(itemId);
    }

    @PatchMapping("/{orderId}/items/{itemId}")
    public OrderItemResponse updateOrderItem(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("itemId") UUID itemId,
            @Valid @RequestBody UpdateOrderItemRequest request
    ) {
        requireOrderItem(orderId, itemId);
        var item = items.updateOrderItem(itemId, request.quantity(), request.purchasePrice());
        return OrderItemResponse.of(item);
    }

    @GetMapping("/{orderId}/items")
    public List<OrderItemResponse> getOrderItems(@PathVariable("orderId") UUID orderId) {
        return items.getAllByOrder(orderId).stream()
                .map(OrderItemResponse::of)
                .toList();
    }

    @GetMapping("/{orderId}/items/{itemId}")
    public OrderItemResponse getOrderItem(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("itemId") UUID itemId
    ) {
        return OrderItemResponse.of(requireOrderItem(orderId, itemId));
    }

    private OrderItem requireOrderItem(UUID orderId, UUID itemId) {
        return items.getOrderItem(itemId)
                .filter(item -> item.getOrder().getId().equals(orderId))
                .orElseThrow(() -> new OrderItemNotFoundException("Позиция заказа не найдена."));
    }
}
