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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Заказы", description = "Заказы и их позиции. Доступно USER, MANAGER и ADMIN после смены временного пароля; изменение статуса требует MANAGER или ADMIN.")
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
    @Operation(summary = "Получить заказ по UUID", description = "Ответ включает totalSum — сумму quantity × purchasePrice по позициям заказа. Отсутствующий или удалённый заказ — 404.")
    public OrderResponse getOrder(
            @PathVariable("id") UUID id
    ) {
        return search.getById(id);
    }
    @GetMapping
    @Operation(summary = "Получить страницу заказов", description = "Необязательные query-фильтры userId, status, createdFrom, createdBefore, minTotal и maxTotal объединяются условием И. "
            + "Даты — ISO-8601, обе границы включительны; createdFrom должен быть раньше createdBefore. Границы суммы включительны, неотрицательны, minTotal ≤ maxTotal. "
            + "page с 0, size от 1 до 100; по умолчанию 0 и 20. sort: createdAt, updatedAt, status; пример sort=createdAt,desc. "
            + "Ответ содержит content и page с size, number, totalElements и totalPages; каждый заказ включает totalSum.")
    public PagedModel<OrderResponse> getOrders(
            @ParameterObject @Valid @ModelAttribute OrderFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @ParameterObject Sort sort
    ) {
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @PostMapping
    @Operation(summary = "Создать заказ", description = "Обязательны userId и status; notes необязателен. Создаёт заказ без позиций; добавляйте их отдельным запросом. "
            + "Ответ содержит созданный заказ и заголовок Location. Несуществующий пользователь — 404.")
    @ApiResponse(responseCode = "201", description = "Заказ создан")
    @Transactional
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        var order = orders.createOrder(request.userId(), request.status(), request.notes());
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
                .body(search.getById(order.getId()));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменить заказ", description = "Обновляет переданные status и notes; null сохраняет прежнее значение. Изменение статуса доступно только MANAGER и ADMIN и записывается в историю. "
            + "Переход в DELIVERED списывает товары со складов; статус доставленного заказа изменить нельзя. Недостаточный остаток отклоняет операцию.")
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
    @Operation(summary = "Удалить заказ", description = "Выполняет мягкое удаление заказа, его позиций и записей истории смены статуса. Повторный вызов для отсутствующего заказа также возвращает 204.")
    @ApiResponse(responseCode = "204", description = "Заказ удалён или уже отсутствует")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable("id") UUID id) {
        orders.deleteOrder(id);
    }

    @PostMapping("/{orderId}/items")
    @Operation(summary = "Добавить позицию заказа", description = "Обязательны productId, положительное quantity и неотрицательная purchasePrice. "
            + "Доставленный заказ изменять нельзя; для статусов с резервированием товаров проверяется доступный остаток. Ответ содержит позицию и заголовок Location.")
    @ApiResponse(responseCode = "201", description = "Позиция заказа создана")
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
    @Operation(summary = "Удалить позицию заказа", description = "Выполняет мягкое удаление позиции. Позиция должна принадлежать указанному заказу, иначе 404. Доставленный заказ изменять нельзя.")
    @ApiResponse(responseCode = "204", description = "Позиция заказа удалена")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrderItem(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("itemId") UUID itemId
    ) {
        requireOrderItem(orderId, itemId);
        items.deleteOrderItem(itemId);
    }

    @PatchMapping("/{orderId}/items/{itemId}")
    @Operation(summary = "Изменить позицию заказа", description = "Обновляет переданные quantity и purchasePrice; null сохраняет прежнее значение. Количество положительно, цена неотрицательна. "
            + "Проверяются принадлежность позиции заказу и доступный остаток; доставленный заказ изменять нельзя.")
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
    @Operation(summary = "Получить позиции заказа", description = "Возвращает список без пагинации. Отсутствующий или удалённый заказ — 404.")
    public List<OrderItemResponse> getOrderItems(@PathVariable("orderId") UUID orderId) {
        return items.getAllByOrder(orderId).stream()
                .map(OrderItemResponse::of)
                .toList();
    }

    @GetMapping("/{orderId}/items/{itemId}")
    @Operation(summary = "Получить позицию заказа по UUID", description = "Позиция должна принадлежать указанному заказу. Отсутствующая, удалённая или принадлежащая другому заказу позиция — 404.")
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
