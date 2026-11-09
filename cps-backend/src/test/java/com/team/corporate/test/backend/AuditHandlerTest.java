package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditHandlerTest extends BackendIntegrationTest {

    @Test
    void auditReturnsEmptyPageForCleanDatabase() throws Exception {
        var result = mvc.perform(get("/api/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andReturn();
        assertThat(result.getResponse().getHeader("X-Correlation-ID")).isNotNull();
    }

    @Test
    void auditRecordsStatusChangesAndAppearsInList() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID manager = user("manager", "MANAGER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 10, 201);
        UUID orderId = id(order(buyer, null, line(product, 2)));
        changeStatus(orderId, manager, "APPROVED");

        mvc.perform(get("/api/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].entityType").value("ORDER"))
                .andExpect(jsonPath("$.items[0].entityUuid").value(orderId.toString()))
                .andExpect(jsonPath("$.items[0].eventType").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$.items[0].changedByUsername").value("manager"))
                .andExpect(jsonPath("$.items[0].correlationId").isNotEmpty());
    }

    @Test
    void auditFiltersByEventType() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID manager = user("manager", "MANAGER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 10, 201);
        UUID order1 = id(order(buyer, null, line(product, 1)));
        UUID order2 = id(order(buyer, null, line(product, 1)));
        changeStatus(order1, manager, "APPROVED");
        changeStatus(order2, manager, "CANCELLED");

        mvc.perform(get("/api/admin/audit").param("eventType", "STATUS_CHANGED"))
                .andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/admin/audit").param("eventType", "LOGIN"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void auditPaginatesCorrectly() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID manager = user("manager", "MANAGER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 50, 201);
        for (int i = 0; i < 5; i++) {
            UUID orderId = id(order(buyer, null, line(product, 1)));
            changeStatus(orderId, manager, "APPROVED");
        }

        mvc.perform(get("/api/admin/audit").param("page", "0").param("size", "2"))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.items.length()").value(2));

        mvc.perform(get("/api/admin/audit").param("page", "2").param("size", "2"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void auditCorrelationIdLinksRequestToLogEntry() throws Exception {
        UUID buyer = user("buyer", "USER");
        UUID manager = user("manager", "MANAGER");
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 10, 201);
        UUID orderId = id(order(buyer, null, line(product, 1)));

        String customId = "test-correlation-12345";
        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("X-Correlation-ID", customId)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(fields("managerId", manager, "status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", customId));

        mvc.perform(get("/api/admin/audit").param("correlationId", customId))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].correlationId").value(customId));
    }
}