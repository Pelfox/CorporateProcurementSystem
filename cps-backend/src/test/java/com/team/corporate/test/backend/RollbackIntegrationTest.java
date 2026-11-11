package com.team.corporate.test.backend;

import com.team.corporate.core.entities.AuditLog;
import com.team.corporate.core.entities.OrderItem;
import com.team.corporate.core.repositories.AuditLogsRepository;
import com.team.corporate.core.repositories.OrderItemsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RollbackIntegrationTest extends BackendIntegrationTest {
    @MockitoSpyBean private OrderItemsRepository items;
    @MockitoSpyBean private AuditLogsRepository audit;

    @Test
    void failureOnSecondItemRollsBackPreviouslyPersistedOrderAndFirstItem() throws Exception {
        var buyer = user("Buyer", "USER");
        var category = category("Category");
        var first = product("First", "ONE", category, "1");
        var second = product("Second", "TWO", category, "2");
        var warehouse = warehouse("Warehouse");
        stock(warehouse, first, 5, 201);
        stock(warehouse, second, 5, 201);
        var calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) throw new IllegalArgumentException("Test item write failure");
            return invocation.callRealMethod();
        }).when(items).add(any(OrderItem.class));

        json(post("/api/orders"), fields("userId", buyer, "items", List.of(line(first, 2), line(second, 3))))
                .andExpect(status().isConflict());
        assertThat(calls.get()).isEqualTo(2);
        assertThat(count("orders")).isZero();
        assertThat(count("order_items")).isZero();
        assertThat(available(first, null)).isEqualTo(5);
        assertThat(available(second, null)).isEqualTo(5);
        doCallRealMethod().when(items).add(any(OrderItem.class));
        order(buyer, null, line(first, 2), line(second, 3));
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("order_items")).isEqualTo(2);
    }

    @Test
    void auditFailureRollsBackDeliveryDeductionsInEveryWarehouseAndAllowsRetry() throws Exception {
        var buyer = user("Buyer", "USER");
        var manager = user("Manager", "MANAGER");
        var product = product("Product", "SKU", category("Category"), "1");
        var first = id(stock(warehouse("First"), product, 3, 201));
        var second = id(stock(warehouse("Second"), product, 4, 201));
        var order = id(order(buyer, null, line(product, 4)));
        doThrow(new IllegalArgumentException("Test audit write failure")).when(audit).add(any(AuditLog.class));

        json(patch("/api/orders/" + order + "/status"), fields("managerId", manager, "status", "DELIVERED"))
                .andExpect(status().isConflict());
        assertThat(read("/api/orders/" + order).path("status").asString()).isEqualTo("CREATED");
        assertThat(read("/api/inventory/" + first).path("quantity").asInt()).isEqualTo(3);
        assertThat(read("/api/inventory/" + second).path("quantity").asInt()).isEqualTo(4);
        assertThat(count("audit_logs")).isZero();
        assertThat(count("order_items")).isEqualTo(1);
        assertThat(available(product, null)).isEqualTo(3);
        doCallRealMethod().when(audit).add(any(AuditLog.class));
        changeStatus(order, manager, "DELIVERED");
        assertThat(physical(product)).isEqualTo(3);
        assertThat(count("audit_logs")).isEqualTo(1);
    }
}
