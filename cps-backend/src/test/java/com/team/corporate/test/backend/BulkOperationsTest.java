package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BulkOperationsTest extends LegacyBusinessIntegrationTest {

    @Test
    void bulkArchiveArchivesMultipleOrders() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 20, 201);
        UUID order1 = id(order(buyer, null, line(product, 1)));
        UUID order2 = id(order(buyer, null, line(product, 1)));
        UUID order3 = id(order(buyer, null, line(product, 1)));

        json(post("/api/orders/bulk").param("actingUserId", buyer.toString()),
                fields("action", "ARCHIVE", "orderIds", List.of(order1, order2, order3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSucceeded").value(3))
                .andExpect(jsonPath("$.totalFailed").value(0));

        assertThat(read("/api/orders/" + order1).path("archived").asBoolean()).isTrue();
        assertThat(read("/api/orders/" + order2).path("archived").asBoolean()).isTrue();
        assertThat(read("/api/orders/" + order3).path("archived").asBoolean()).isTrue();

        mvc.perform(get("/api/admin/audit").param("eventType", "ARCHIVED"))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void bulkArchiveReportsFailuresPerUuid() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 10, 201);
        UUID existing = id(order(buyer, null, line(product, 1)));
        UUID nonExisting = UUID.randomUUID();

        json(post("/api/orders/bulk").param("actingUserId", buyer.toString()),
                fields("action", "ARCHIVE", "orderIds", List.of(existing, nonExisting)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSucceeded").value(1))
                .andExpect(jsonPath("$.totalFailed").value(1));
        assertThat(read("/api/orders/" + existing).path("archived").asBoolean()).isTrue();
    }

    @Test
    void bulkRestoreRestoresArchivedOrders() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 20, 201);
        UUID order1 = id(order(buyer, null, line(product, 1)));
        UUID order2 = id(order(buyer, null, line(product, 1)));

        json(post("/api/orders/bulk").param("actingUserId", buyer.toString()),
                fields("action", "ARCHIVE", "orderIds", List.of(order1, order2)))
                .andExpect(status().isOk());

        json(post("/api/orders/bulk").param("actingUserId", buyer.toString()),
                fields("action", "RESTORE", "orderIds", List.of(order1, order2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSucceeded").value(2));

        assertThat(read("/api/orders/" + order1).path("archived").asBoolean()).isFalse();
    }

    @Test
    void bulkChangeStatusChangesMultipleOrders() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID manager = user("manager", "MANAGER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 30, 201);
        UUID order1 = id(order(buyer, null, line(product, 1)));
        UUID order2 = id(order(buyer, null, line(product, 1)));

        json(post("/api/orders/bulk").param("actingUserId", manager.toString()),
                fields("action", "CHANGE_STATUS", "orderIds", List.of(order1, order2),
                        "status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSucceeded").value(2));

        assertThat(read("/api/orders/" + order1).path("status").asString()).isEqualTo("APPROVED");
    }

    @Test
    void bulkAddTagAddsTagToMultipleOrders() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 20, 201);
        UUID order1 = id(order(buyer, null, line(product, 1)));
        UUID order2 = id(order(buyer, null, line(product, 1)));

        json(post("/api/orders/bulk").param("actingUserId", buyer.toString()),
                fields("action", "ADD_TAG", "orderIds", List.of(order1, order2),
                        "tagName", "срочный"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSucceeded").value(2));

        assertThat(read("/api/orders/" + order1).path("tags").toString()).contains("срочный");
    }

    @Test
    void permanentDeleteRemovesOrderCompletely() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID manager = user("manager", "MANAGER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 10, 201);
        UUID orderId = id(order(buyer, null, line(product, 1)));

        mvc.perform(delete("/api/orders/" + orderId + "/permanent").param("actingUserId", manager.toString()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/orders/" + orderId)).andExpect(status().isNotFound());
        assertThat(count("orders")).isZero();

        mvc.perform(get("/api/admin/audit").param("eventType", "PERMANENTLY_DELETED"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void permanentDeleteFailsForRegularUser() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 10, 201);
        UUID orderId = id(order(buyer, null, line(product, 1)));

        mvc.perform(delete("/api/orders/" + orderId + "/permanent").param("actingUserId", buyer.toString()))
                .andExpect(status().isConflict());
        assertThat(count("orders")).isEqualTo(1);
    }

    @Test
    void bulkOperationRejectsEmptyList() throws Exception {
        UUID buyer = user("buyer", "USER");
        json(post("/api/orders/bulk").param("actingUserId", buyer.toString()),
                fields("action", "ARCHIVE", "orderIds", List.of()))
                .andExpect(status().isBadRequest());
    }
}