package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrdersHandlerTest extends BackendIntegrationTest {
    @Test
    void createsOrderAggregatesRepeatedProductsAndCalculatesTotals() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "3.25");
        stock(warehouse("Main"), product, 10, 201);
        var order = order(buyer, "  Office supplies  ", line(product, 2), line(product, 3));
        assertThat(order.path("userId").asString()).isEqualTo(buyer.toString());
        assertThat(order.path("username").asString()).isEqualTo("buyer");
        assertThat(order.path("status").asString()).isEqualTo("CREATED");
        assertThat(order.path("notes").asString()).isEqualTo("Office supplies");
        assertThat(order.path("lineCount").asInt()).isEqualTo(1);
        assertThat(order.path("totalQuantity").asLong()).isEqualTo(5);
        assertThat(order.path("total").decimalValue()).isEqualByComparingTo("16.25");
        assertThat(Instant.parse(order.path("updatedAt").asString())).isAfterOrEqualTo(Instant.parse(order.path("createdAt").asString()));
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("order_items")).isEqualTo(1);
        assertThat(read("/api/orders/" + id(order) + "/items").get(0).path("productId").asString()).isEqualTo(product.toString());
        assertThat(read("/api/orders/" + id(order) + "/audit").size()).isZero();
        assertThat(available(product, null)).isEqualTo(5);
    }

    @Test
    void replacingCartPreservesOldPriceAndItemIdAndAddsNewProductsAtCurrentPrice() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID category = category("Office");
        UUID first = product("First", "FIRST", category, "10.00");
        UUID removed = product("Removed", "REMOVED", category, "20.00");
        UUID added = product("Added", "ADDED", category, "30.00");
        UUID warehouse = warehouse("Main");
        for (UUID product : List.of(first, removed, added)) stock(warehouse, product, 10, 201);
        var original = order(buyer, "Original", line(first, 2), line(removed, 1));
        UUID order = id(original);
        UUID firstItem = itemId(original, first);
        json(put("/api/products/" + first), productFields("First", "FIRST", category, "99.00")).andExpect(status().isOk());
        var updated = tree(json(put("/api/orders/" + order), fields("userId", buyer,
                "items", List.of(line(first, 3), line(added, 2)), "notes", "Updated")).andExpect(status().isOk()));
        assertThat(itemId(updated, first)).isEqualTo(firstItem);
        assertThat(updated.path("total").decimalValue()).isEqualByComparingTo("90.00");
        assertThat(updated.path("lineCount").asInt()).isEqualTo(2);
        assertThat(updated.path("totalQuantity").asLong()).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT purchase_price FROM order_items WHERE id = ?", java.math.BigDecimal.class, firstItem))
                .isEqualByComparingTo("10.00");
        assertThat(count("order_items")).isEqualTo(2);
        assertThat(available(first, null)).isEqualTo(7);
        assertThat(available(removed, null)).isEqualTo(10);
        assertThat(available(added, null)).isEqualTo(8);
        assertThat(read("/api/orders/" + order).path("notes").asString()).isEqualTo("Updated");
    }

    @Test
    void addsChangesAndRemovesItemsButKeepsLastOne() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID category = category("Office");
        UUID first = product("First", "FIRST", category, "10.00");
        UUID second = product("Second", "SECOND", category, "2.00");
        UUID warehouse = warehouse("Main");
        stock(warehouse, first, 10, 201);
        stock(warehouse, second, 10, 201);
        UUID order = id(order(buyer, null, line(first, 2)));
        var added = tree(json(post("/api/orders/" + order + "/items"), fields("userId", buyer, "productId", second, "quantity", 1))
                .andExpect(status().isOk()));
        UUID firstItem = itemId(added, first);
        UUID secondItem = itemId(added, second);
        json(put("/api/orders/" + order + "/items/" + firstItem), fields("userId", buyer, "quantity", 4))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(5));
        mvc.perform(delete("/api/orders/" + order + "/items/" + secondItem).param("userId", buyer.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lineCount").value(1));
        mvc.perform(delete("/api/orders/" + order + "/items/" + firstItem).param("userId", buyer.toString()))
                .andExpect(status().isConflict());
        assertThat(count("order_items")).isEqualTo(1);
        assertThat(read("/api/orders/" + order).path("totalQuantity").asLong()).isEqualTo(4);
    }

    @Test
    void notesCanBeChangedAndClearedAndWrongOwnerCannotEdit() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID other = user("other", "MANAGER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 3, 201);
        UUID order = id(order(buyer, "Original", line(product, 1)));
        json(patch("/api/orders/" + order + "/notes"), fields("userId", other, "notes", "Denied"))
                .andExpect(status().isConflict());
        assertThat(read("/api/orders/" + order).path("notes").asString()).isEqualTo("Original");
        json(patch("/api/orders/" + order + "/notes"), fields("userId", buyer, "notes", " Updated "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notes").value("Updated"));
        json(patch("/api/orders/" + order + "/notes"), fields("userId", buyer, "notes", null))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notes").isEmpty());
        assertThat(jdbc.queryForObject("SELECT notes FROM orders WHERE id = ?", String.class, order)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "DELIVERED", "CANCELLED"})
    void onlyCreatedOrdersCanBeEdited(String state) throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID manager = user("manager", "MANAGER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 10, 201);
        var original = order(buyer, "Original", line(product, 2));
        UUID order = id(original);
        UUID item = itemId(original, product);
        changeStatus(order, manager, state);
        json(put("/api/orders/" + order), fields("userId", buyer, "items", List.of(line(product, 3)), "notes", "Denied"))
                .andExpect(status().isConflict());
        json(patch("/api/orders/" + order + "/notes"), fields("userId", buyer, "notes", "Denied"))
                .andExpect(status().isConflict());
        json(post("/api/orders/" + order + "/items"), fields("userId", buyer, "productId", product, "quantity", 1))
                .andExpect(status().isConflict());
        json(put("/api/orders/" + order + "/items/" + item), fields("userId", buyer, "quantity", 3))
                .andExpect(status().isConflict());
        var unchanged = read("/api/orders/" + order);
        assertThat(unchanged.path("totalQuantity").asLong()).isEqualTo(2);
        assertThat(unchanged.path("notes").asString()).isEqualTo("Original");
    }

    @Test
    void itemMustExistAndBelongToSelectedOrder() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 10, 201);
        UUID first = id(order(buyer, null, line(product, 1)));
        var second = order(buyer, null, line(product, 2));
        for (UUID item : List.of(itemId(second, product), UUID.randomUUID())) {
            json(put("/api/orders/" + first + "/items/" + item), fields("userId", buyer, "quantity", 5))
                    .andExpect(status().isNotFound());
        }
        assertThat(read("/api/orders/" + first).path("totalQuantity").asLong()).isEqualTo(1);
        assertThat(read("/api/orders/" + id(second)).path("totalQuantity").asLong()).isEqualTo(2);
    }

    @Test
    void orderSearchFiltersSortsAndIncludesUtcDateBoundaries() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID other = user("other", "USER");
        UUID manager = user("manager", "MANAGER");
        UUID product = product("Paper", "PAPER", category("Office"), "10.00");
        stock(warehouse("Main"), product, 20, 201);
        UUID first = id(order(buyer, "Alpha", line(product, 1)));
        UUID second = id(order(buyer, "Special", line(product, 3)));
        UUID third = id(order(other, "Other", line(product, 2)));
        jdbc.update("UPDATE orders SET created_at = TIMESTAMP WITH TIME ZONE '2026-01-01 23:59:59+00' WHERE id = ?", first);
        jdbc.update("UPDATE orders SET created_at = TIMESTAMP WITH TIME ZONE '2026-01-02 00:00:00+00' WHERE id = ?", second);
        jdbc.update("UPDATE orders SET created_at = TIMESTAMP WITH TIME ZONE '2026-01-03 00:00:00+00' WHERE id = ?", third);
        changeStatus(second, manager, "APPROVED");
        mvc.perform(get("/api/orders").param("userId", buyer.toString()).param("q", "SPEC")
                .param("status", "APPROVED").param("dateFrom", "2026-01-02").param("dateTo", "2026-01-02")
                .param("minTotal", "30").param("maxTotal", "30"))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(second.toString()));
        mvc.perform(get("/api/orders").param("q", first.toString().substring(0, 8))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/orders").param("sort", "CREATED_ASC")).andExpect(jsonPath("$[0].id").value(first.toString()));
        mvc.perform(get("/api/orders")).andExpect(jsonPath("$[0].id").value(third.toString()));
        mvc.perform(get("/api/orders").param("sort", "TOTAL_ASC")).andExpect(jsonPath("$[0].id").value(first.toString()));
        mvc.perform(get("/api/orders").param("sort", "TOTAL_DESC")).andExpect(jsonPath("$[0].id").value(second.toString()));
        mvc.perform(get("/api/orders").param("userId", manager.toString())).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/orders").param("userId", UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    void deletingActiveOrderRemovesChildrenAuditAndReservation() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 5, 201);
        UUID order = id(order(buyer, null, line(product, 4)));
        changeStatus(order, user("manager", "MANAGER"), "APPROVED");
        assertThat(count("audit_logs")).isEqualTo(1);
        mvc.perform(delete("/api/orders/" + order)).andExpect(status().isNoContent());
        assertThat(count("orders")).isZero();
        assertThat(count("order_items")).isZero();
        assertThat(count("audit_logs")).isZero();
        assertThat(physical(product)).isEqualTo(5);
        assertThat(available(product, null)).isEqualTo(5);
    }

    @Test
    void missingOrdersReturn404ForAllOperations() throws Exception {
        UUID order = UUID.randomUUID();
        UUID user = user("user", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        for (String suffix : List.of("", "/items", "/audit")) mvc.perform(get("/api/orders/" + order + suffix)).andExpect(status().isNotFound());
        json(put("/api/orders/" + order), fields("userId", user, "items", List.of(line(product, 1))))
                .andExpect(status().isNotFound());
        json(patch("/api/orders/" + order + "/notes"), fields("userId", user, "notes", "X")).andExpect(status().isNotFound());
        json(patch("/api/orders/" + order + "/status"), fields("managerId", user, "status", "APPROVED"))
                .andExpect(status().isNotFound());
        json(post("/api/orders/" + order + "/items"), fields("userId", user, "productId", product, "quantity", 1))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/orders/" + order)).andExpect(status().isNotFound());
        assertThat(count("orders")).isZero();
    }
}
