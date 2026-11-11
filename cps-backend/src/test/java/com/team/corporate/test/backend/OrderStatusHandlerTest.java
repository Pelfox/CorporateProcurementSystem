package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderStatusHandlerTest extends BackendIntegrationTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "CREATED,CREATED", "CREATED,APPROVED", "CREATED,DELIVERED", "CREATED,CANCELLED",
            "APPROVED,CREATED", "APPROVED,APPROVED", "APPROVED,DELIVERED", "APPROVED,CANCELLED",
            "CANCELLED,CREATED", "CANCELLED,APPROVED", "CANCELLED,DELIVERED", "CANCELLED,CANCELLED",
            "DELIVERED,CREATED", "DELIVERED,APPROVED", "DELIVERED,DELIVERED", "DELIVERED,CANCELLED"
    })
    void statusMatrixPreservesStockAndWritesOnlyActualTransitions(String from, String to) throws Exception {
        var buyer = user("Buyer", "USER");
        var manager = user("Manager", "MANAGER");
        var product = product("Product", "SKU", category("Category"), "2");
        stock(warehouse("Warehouse"), product, 10, 201);
        var order = id(order(buyer, null, line(product, 2)));
        if (!from.equals("CREATED")) changeStatus(order, manager, from);
        long auditsBefore = count("audit_logs");
        long stockBefore = physical(product);
        boolean forbidden = from.equals("DELIVERED") && !to.equals(from);

        json(patch("/api/orders/" + order + "/status"), fields("managerId", manager, "status", to))
                .andExpect(status().is(forbidden ? 409 : 200));

        String expected = forbidden ? from : to;
        assertThat(read("/api/orders/" + order).path("status").asString()).isEqualTo(expected);
        assertThat(count("audit_logs")).isEqualTo(auditsBefore + (!forbidden && !from.equals(to) ? 1 : 0));
        long expectedStock = !forbidden && !from.equals(to) && to.equals("DELIVERED") ? stockBefore - 2 : stockBefore;
        assertThat(physical(product)).isEqualTo(expectedStock);
        assertThat(available(product, null)).isEqualTo(expectedStock - (expected.equals("CREATED") || expected.equals("APPROVED") ? 2 : 0));
    }

    @Test
    void auditContainsActorStatusesAndChronologicalTimestamps() throws Exception {
        var buyer = user("Buyer", "USER");
        var manager = user("Manager", "MANAGER");
        var product = product("Product", "SKU", category("Category"), "1");
        stock(warehouse("Warehouse"), product, 10, 201);
        var order = id(order(buyer, null, line(product, 2)));
        var before = Instant.now();
        changeStatus(order, manager, "APPROVED");
        changeStatus(order, manager, "CANCELLED");
        changeStatus(order, manager, "CANCELLED");
        var audit = read("/api/orders/" + order + "/audit");
        assertThat(audit.size()).isEqualTo(2);
        assertThat(audit.get(0).path("oldStatus").asString()).isEqualTo("CREATED");
        assertThat(audit.get(0).path("newStatus").asString()).isEqualTo("APPROVED");
        assertThat(audit.get(1).path("oldStatus").asString()).isEqualTo("APPROVED");
        assertThat(audit.get(1).path("newStatus").asString()).isEqualTo("CANCELLED");
        Instant previous = before;
        for (var entry : audit) {
            assertThat(entry.path("orderId").asString()).isEqualTo(order.toString());
            assertThat(entry.path("changedBy").asString()).isEqualTo(manager.toString());
            assertThat(entry.path("changedByUsername").asString()).isEqualTo("Manager");
            Instant time = Instant.parse(entry.path("changedAt").asString());
            assertThat(time).isBetween(previous, Instant.now());
            previous = time;
            assertThat(jdbc.queryForObject("SELECT changed_by FROM audit_logs WHERE id = ?", UUID.class, id(entry))).isEqualTo(manager);
        }
    }

    @Test
    void statusRequiresAnExistingManagerWithoutChangingOrderOnFailure() throws Exception {
        var buyer = user("Buyer", "USER");
        var product = product("Product", "SKU", category("Category"), "1");
        stock(warehouse("Warehouse"), product, 5, 201);
        var order = id(order(buyer, null, line(product, 2)));
        for (var actor : new UUID[]{buyer, UUID.randomUUID()}) {
            json(patch("/api/orders/" + order + "/status"), fields("managerId", actor, "status", "DELIVERED"))
                    .andExpect(status().isConflict());
        }
        assertThat(read("/api/orders/" + order).path("status").asString()).isEqualTo("CREATED");
        assertThat(count("audit_logs")).isZero();
        assertThat(physical(product)).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREATED", "APPROVED", "DELIVERED"})
    void cannotReopenCancelledOrderWhenAnotherOrderHasReservedStock(String status) throws Exception {
        var buyer = user("Buyer", "USER");
        var manager = user("Manager", "MANAGER");
        var product = product("Product", "SKU", category("Category"), "1");
        stock(warehouse("Warehouse"), product, 5, 201);
        var cancelled = id(order(buyer, null, line(product, 4)));
        changeStatus(cancelled, manager, "CANCELLED");
        order(buyer, null, line(product, 3));
        json(patch("/api/orders/" + cancelled + "/status"), fields("managerId", manager, "status", status))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
        assertThat(read("/api/orders/" + cancelled).path("status").asString()).isEqualTo("CANCELLED");
        assertThat(count("audit_logs")).isEqualTo(1);
        assertThat(available(product, null)).isEqualTo(2);
        assertThat(physical(product)).isEqualTo(5);
    }
}
