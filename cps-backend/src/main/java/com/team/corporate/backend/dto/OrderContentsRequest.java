package com.team.corporate.backend.dto;

import com.team.corporate.core.services.OrdersService;
import com.team.corporate.core.utils.EntityValidation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record OrderContentsRequest(
        @NotEmpty @Size(max = 1000) List<@NotNull @Valid Line> items,
        @Size(max = EntityValidation.TEXT_MAX_LENGTH) String notes
) {
    public record Line(@NotNull UUID productId,
                       @NotNull @Positive @Max(EntityValidation.ORDER_LINE_MAX_QUANTITY) Integer quantity) {}

    public List<OrdersService.Line> lines() {
        return items.stream().map(item -> new OrdersService.Line(item.productId(), item.quantity())).toList();
    }
}
