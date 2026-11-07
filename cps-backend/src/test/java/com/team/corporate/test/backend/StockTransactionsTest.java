package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockTransactionsTest extends BackendIntegrationTest {
    @Test
    void failedCreationDoesNotLeaveOrderItemsOrReservations() throws Exception {
        var buyer = user("Buyer", "USER");
        var category = category("Category");
        var first = product("First", "ONE", category, "1");
        var second = product("Second", "TWO", category, "2");
        var warehouse = warehouse("Warehouse");
        stock(warehouse, first, 5, 201);
        stock(warehouse, second, 1, 201);
        json(post("/api/orders"), fields("userId", buyer, "items", List.of(line(first, 3), line(second, 2))))
                .andExpect(status().isConflict());
        assertThat(count("orders")).isZero();
        assertThat(count("order_items")).isZero();
        assertThat(available(first, null)).isEqualTo(5);
        assertThat(available(second, null)).isEqualTo(1);
        assertThat(physical(first)).isEqualTo(5);
        order(buyer, null, line(first, 5));
        assertThat(available(first, null)).isZero();
    }

    @Test
    void failedCartReplacementAddAndQuantityChangePreserveExistingCart() throws Exception {
        var buyer = user("Buyer", "USER");
        var category = category("Category");
        var first = product("First", "ONE", category, "10");
        var second = product("Second", "TWO", category, "20");
        var warehouse = warehouse("Warehouse");
        stock(warehouse, first, 5, 201);
        stock(warehouse, second, 1, 201);
        var original = order(buyer, "Original", line(first, 2));
        var order = id(original);
        json(put("/api/orders/" + order), fields("userId", buyer, "notes", "Changed",
                "items", List.of(line(first, 4), line(second, 2)))).andExpect(status().isConflict());
        json(post("/api/orders/" + order + "/items"), fields("userId", buyer, "productId", second, "quantity", 2))
                .andExpect(status().isConflict());
        json(put("/api/orders/" + order + "/items/" + itemId(original, first)), fields("userId", buyer, "quantity", 6))
                .andExpect(status().isConflict());
        assertThat(read("/api/orders/" + order)).isEqualTo(original);
        assertThat(count("order_items")).isEqualTo(1);
        assertThat(available(first, null)).isEqualTo(3);
        assertThat(available(second, null)).isEqualTo(1);
    }

    @Test
    void quantityLimitAppliesToAggregatedDuplicateLines() throws Exception {
        var buyer = user("Buyer", "USER");
        var product = product("Product", "SKU", category("Category"), "1");
        stock(warehouse("Warehouse"), product, 200000, 201);
        json(post("/api/orders"), fields("userId", buyer, "items", List.of(line(product, 60000), line(product, 40001))))
                .andExpect(status().isConflict());
        assertThat(count("orders")).isZero();
        var order = order(buyer, null, line(product, 60000), line(product, 40000));
        assertThat(order.path("totalQuantity").asInt()).isEqualTo(100000);
        json(post("/api/orders/" + id(order) + "/items"), fields("userId", buyer, "productId", product, "quantity", 1))
                .andExpect(status().isConflict());
        assertThat(read("/api/orders/" + id(order)).path("totalQuantity").asInt()).isEqualTo(100000);
        assertThat(available(product, null)).isEqualTo(100000);
    }

    @Test
    void deliveryDeductsAcrossWarehousesOnceAndPreservesOtherReservations() throws Exception {
        var buyer = user("Buyer", "USER");
        var manager = user("Manager", "MANAGER");
        var product = product("Product", "SKU", category("Category"), "1");
        stock(warehouse("First"), product, 3, 201);
        stock(warehouse("Second"), product, 4, 201);
        var delivered = id(order(buyer, null, line(product, 4)));
        var reserved = id(order(buyer, null, line(product, 3)));
        changeStatus(delivered, manager, "DELIVERED");
        assertThat(physical(product)).isEqualTo(3);
        assertThat(available(product, null)).isZero();
        assertThat(available(product, reserved)).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT quantity FROM inventory WHERE product_id = ?", Integer.class, product))
                .allMatch(quantity -> quantity >= 0);
        changeStatus(delivered, manager, "DELIVERED");
        assertThat(physical(product)).isEqualTo(3);
        assertThat(count("audit_logs")).isEqualTo(1);
        mvc.perform(delete("/api/orders/" + delivered)).andExpect(status().isNoContent());
        assertThat(physical(product)).isEqualTo(3);
        assertThat(available(product, null)).isZero();
        changeStatus(reserved, manager, "CANCELLED");
        assertThat(available(product, null)).isEqualTo(3);
    }
}
