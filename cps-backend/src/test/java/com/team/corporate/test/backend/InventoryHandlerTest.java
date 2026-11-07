package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryHandlerTest extends BackendIntegrationTest {
    @Test
    void warehouseCrudSearchAndClearingAddress() throws Exception {
        UUID main = warehouse("Main");
        UUID other = warehouse("Other");
        json(put("/api/warehouses/" + main), fields("name", " Main Updated ", "location", "Moscow"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Main Updated"));
        mvc.perform(get("/api/warehouses").param("q", "MOSC"))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(main.toString()));
        mvc.perform(get("/api/warehouses").param("q", "updated")).andExpect(jsonPath("$.length()").value(1));
        json(put("/api/warehouses/" + main), fields("name", "Main Updated", "location", null))
                .andExpect(status().isOk()).andExpect(jsonPath("$.location").isEmpty());
        assertThat(jdbc.queryForObject("SELECT location FROM warehouses WHERE id = ?", String.class, main)).isNull();
        mvc.perform(delete("/api/warehouses/" + other)).andExpect(status().isNoContent());
        mvc.perform(get("/api/warehouses/" + other)).andExpect(status().isNotFound());
    }

    @Test
    void upsertReplacesAbsoluteQuantityAndKeepsOneRecordPerPair() throws Exception {
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        UUID warehouse = warehouse("Main");
        var created = stock(warehouse, product, 0, 201);
        var updated = stock(warehouse, product, 8, 200);
        assertThat(id(updated)).isEqualTo(id(created));
        assertThat(updated.path("quantity").asInt()).isEqualTo(8);
        assertThat(updated.path("productName").asString()).isEqualTo("Paper");
        assertThat(updated.path("warehouseName").asString()).isEqualTo("Main");
        assertThat(count("inventory")).isEqualTo(1);
        assertThat(physical(product)).isEqualTo(8);
        assertThat(read("/api/inventory/" + id(created)).path("quantity").asInt()).isEqualTo(8);
    }

    @Test
    void inventoryCanBeSelectedByProductWarehouseAndBoth() throws Exception {
        UUID category = category("Office");
        UUID paper = product("Paper", "PAPER", category, "1.00");
        UUID pen = product("Pen", "PEN", category, "2.00");
        UUID first = warehouse("First");
        UUID second = warehouse("Second");
        stock(first, paper, 2, 201);
        stock(second, paper, 3, 201);
        stock(first, pen, 4, 201);
        mvc.perform(get("/api/inventory")).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/inventory").param("productId", paper.toString())).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/inventory").param("warehouseId", first.toString())).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/inventory").param("productId", paper.toString()).param("warehouseId", second.toString()))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].quantity").value(3));
        mvc.perform(get("/api/inventory").param("productId", pen.toString()).param("warehouseId", second.toString()))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/warehouses/" + first + "/inventory")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void warehouseWithEvenZeroQuantityRecordCannotBeDeleted() throws Exception {
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        UUID warehouse = warehouse("Main");
        UUID inventory = id(stock(warehouse, product, 0, 201));
        mvc.perform(delete("/api/warehouses/" + warehouse)).andExpect(status().isConflict());
        assertThat(count("warehouses")).isEqualTo(1);
        assertThat(count("inventory")).isEqualTo(1);
        mvc.perform(delete("/api/inventory/" + inventory)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/warehouses/" + warehouse)).andExpect(status().isNoContent());
    }

    @Test
    void availabilitySumsWarehousesAndExcludesEditingOrderReservation() throws Exception {
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("First"), product, 5, 201);
        stock(warehouse("Second"), product, 7, 201);
        UUID buyer = user("buyer", "USER");
        UUID first = id(order(buyer, null, line(product, 4)));
        UUID second = id(order(buyer, null, line(product, 3)));
        assertThat(physical(product)).isEqualTo(12);
        assertThat(available(product, null)).isEqualTo(5);
        assertThat(available(product, first)).isEqualTo(9);
        assertThat(available(product, second)).isEqualTo(8);
        changeStatus(first, user("manager", "MANAGER"), "APPROVED");
        assertThat(available(product, null)).isEqualTo(5);
    }

    @Test
    void reservedStockCannotBeReducedBelowFloorOrDeleted() throws Exception {
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        UUID first = warehouse("First");
        UUID second = warehouse("Second");
        UUID entry = id(stock(first, product, 6, 201));
        stock(second, product, 4, 201);
        UUID order = id(order(user("buyer", "USER"), null, line(product, 7)));
        stock(first, product, 2, 409);
        assertThat(physical(product)).isEqualTo(10);
        stock(first, product, 3, 200);
        assertThat(physical(product)).isEqualTo(7);
        assertThat(available(product, null)).isZero();
        mvc.perform(delete("/api/inventory/" + entry)).andExpect(status().isConflict());
        assertThat(physical(product)).isEqualTo(7);
        changeStatus(order, user("manager", "MANAGER"), "CANCELLED");
        mvc.perform(delete("/api/inventory/" + entry)).andExpect(status().isNoContent());
        assertThat(physical(product)).isEqualTo(4);
        assertThat(available(product, null)).isEqualTo(4);
    }

    @Test
    void noInventoryMeansZeroAvailabilityAndLargeTotalsDoNotOverflow() throws Exception {
        UUID category = category("Office");
        UUID empty = product("Empty", "EMPTY", category, "0.00");
        UUID stocked = product("Stocked", "STOCKED", category, "0.00");
        stock(warehouse("First"), stocked, Integer.MAX_VALUE, 201);
        stock(warehouse("Second"), stocked, 1, 201);
        assertThat(available(empty, null)).isZero();
        assertThat(physical(stocked)).isEqualTo(2147483648L);
        assertThat(available(stocked, null)).isEqualTo(2147483648L);
    }

    @Test
    void missingWarehouseProductAndInventoryReturn404WithoutWrites() throws Exception {
        UUID missing = UUID.randomUUID();
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        UUID warehouse = warehouse("Main");
        mvc.perform(get("/api/warehouses/" + missing)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/warehouses/" + missing)).andExpect(status().isNotFound());
        json(put("/api/warehouses/" + missing), fields("name", "X", "location", null)).andExpect(status().isNotFound());
        mvc.perform(get("/api/inventory/" + missing)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/inventory/" + missing)).andExpect(status().isNotFound());
        mvc.perform(get("/api/inventory").param("productId", missing.toString())).andExpect(status().isNotFound());
        mvc.perform(get("/api/warehouses/" + missing + "/inventory")).andExpect(status().isNotFound());
        stock(missing, product, 1, 404);
        stock(warehouse, missing, 1, 404);
        assertThat(count("inventory")).isZero();
    }
}
