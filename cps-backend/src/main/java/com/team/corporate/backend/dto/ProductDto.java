package com.team.corporate.backend.dto;

import com.team.corporate.backend.validation.TextValue;
import com.team.corporate.core.entities.Product;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String sku,
        UUID categoryId,
        String categoryName,
        BigDecimal price
) {
    public static ProductDto from(Product product) {
        var category = product.getCategory();
        return new ProductDto(product.getId(), product.getName(), product.getSku(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(), product.getPrice());
    }

    public enum Sort {
        NAME_ASC,
        NAME_DESC,
        PRICE_ASC,
        PRICE_DESC
    }

    public record Request(
            @TextValue(required = true) String name,
            @TextValue(required = true) String sku,
            @NotNull UUID categoryId,
            @NotNull @DecimalMin("0") @DecimalMax("99999999.99")
            @Digits(integer = 8, fraction = 2) BigDecimal price
    ) {
    }

    public record Query(
            @TextValue String q,
            UUID categoryId,
            @DecimalMin("0") BigDecimal minPrice,
            @DecimalMin("0") BigDecimal maxPrice,
            Sort sort
    ) {
        public Query {
            if (sort == null) {
                sort = Sort.NAME_ASC;
            }
        }

        @AssertTrue(message = "minPrice не может превышать maxPrice")
        public boolean isPriceRangeValid() {
            return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
        }
    }
}
