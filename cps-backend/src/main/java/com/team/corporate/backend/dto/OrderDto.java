package com.team.corporate.backend.dto;

import com.team.corporate.backend.validation.TextValue;
import com.team.corporate.core.entities.Order;
import com.team.corporate.core.entities.OrderItem;
import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.services.OrdersService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        UUID userId,
        String username,
        OrderStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        int lineCount,
        long totalQuantity,
        BigDecimal total,
        List<Item> items
) {
    public static OrderDto from(@NotNull Order order, List<OrderItem> lines) {
        var items = lines.stream().sorted(Comparator.comparing(OrderItem::getId)).map(Item::from).toList();
        return new OrderDto(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getUsername(),
                order.getStatus(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items.size(),
                items.stream().mapToLong(Item::quantity).sum(),
                items.stream().map(Item::total).reduce(BigDecimal.ZERO, BigDecimal::add),
                items
        );
    }

    public enum Sort {
        CREATED_ASC,
        CREATED_DESC,
        TOTAL_ASC,
        TOTAL_DESC
    }

    public record Item(
            UUID id,
            UUID productId,
            String productName,
            String sku,
            int quantity,
            BigDecimal purchasePrice,
            BigDecimal total
    ) {
        static Item from(OrderItem item) {
            return new Item(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getSku(),
                    item.getQuantity(),
                    item.getPurchasePrice(),
                    item.getPurchasePrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }
    }

    public record Request(
            @NotNull UUID userId,
            @NotEmpty List<@NotNull @Valid LineRequest> items,
            @TextValue String notes
    ) {
        public List<OrdersService.Line> lines() {
            return items.stream().map(LineRequest::line).toList();
        }
    }

    public record LineRequest(@NotNull UUID productId, @NotNull @Min(1) @Max(100000) Integer quantity) {
        public OrdersService.Line line() {
            return new OrdersService.Line(productId, quantity);
        }
    }

    public record AddItemRequest(
            @NotNull UUID userId,
            @NotNull UUID productId,
            @NotNull @Min(1) @Max(100000) Integer quantity
    ) {
        public OrdersService.Line line() {
            return new OrdersService.Line(productId, quantity);
        }
    }

    public record QuantityRequest(@NotNull UUID userId, @NotNull @Min(1) @Max(100000) Integer quantity) {
    }

    public record NotesRequest(@NotNull UUID userId, @TextValue String notes) {
    }

    public record StatusRequest(@NotNull UUID managerId, @NotNull OrderStatus status) {
    }

    public record Query(
            @TextValue String q,
            UUID userId,
            OrderStatus status,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @DecimalMin("0") BigDecimal minTotal,
            @DecimalMin("0") BigDecimal maxTotal,
            Sort sort
    ) {
        public Query {
            if (sort == null) {
                sort = Sort.CREATED_DESC;
            }
        }

        @AssertTrue(message = "dateFrom не может быть позже dateTo")
        public boolean isDateRangeValid() {
            return dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo);
        }

        @AssertTrue(message = "minTotal не может превышать maxTotal")
        public boolean isTotalRangeValid() {
            return minTotal == null || maxTotal == null || minTotal.compareTo(maxTotal) <= 0;
        }
    }
}
