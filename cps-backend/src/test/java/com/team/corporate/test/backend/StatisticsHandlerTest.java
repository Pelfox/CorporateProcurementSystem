package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsHandlerTest extends BackendIntegrationTest {
    @Test
    void emptyDatabaseReturnsZeroSummaryAndEmptyWarehouseList() throws Exception {
        var stats = read("/api/statistics");
        for (String field : List.of("totalUsers", "totalProducts", "totalCategories", "totalWarehouses", "totalOrders",
                "activeOrders", "createdOrders", "approvedOrders", "deliveredOrders", "cancelledOrders", "totalStockQuantity"))
            assertThat(stats.path(field).asLong()).as(field).isZero();
        assertThat(stats.path("totalOrderValue").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(read("/api/statistics/warehouses").size()).isZero();
    }

    @Test
    void summaryCountsEveryStatusAndWarehouseStockIncludesEmptyWarehouses() throws Exception {
        var buyer = user("Buyer", "USER");
        var manager = user("Manager", "MANAGER");
        var category = category("Category");
        var first = product("First", "ONE", category, "10");
        var second = product("Second", "TWO", category, "2.50");
        var alpha = warehouse("Alpha");
        var beta = warehouse("Beta");
        warehouse("Empty");
        stock(alpha, first, 10, 201);
        stock(beta, second, 8, 201);
        order(buyer, null, line(first, 2)); // 20, CREATED
        var approved = id(order(buyer, null, line(first, 3))); // 30
        var delivered = id(order(buyer, null, line(second, 4))); // 10
        var cancelled = id(order(buyer, null, line(second, 2))); // 5
        changeStatus(approved, manager, "APPROVED");
        changeStatus(delivered, manager, "DELIVERED");
        changeStatus(cancelled, manager, "CANCELLED");

        var stats = read("/api/statistics");
        assertThat(stats.path("totalUsers").asLong()).isEqualTo(2);
        assertThat(stats.path("totalProducts").asLong()).isEqualTo(2);
        assertThat(stats.path("totalCategories").asLong()).isEqualTo(1);
        assertThat(stats.path("totalWarehouses").asLong()).isEqualTo(3);
        assertThat(stats.path("totalOrders").asLong()).isEqualTo(4);
        assertThat(stats.path("activeOrders").asLong()).isEqualTo(2);
        for (String field : List.of("createdOrders", "approvedOrders", "deliveredOrders", "cancelledOrders"))
            assertThat(stats.path(field).asLong()).as(field).isEqualTo(1);
        assertThat(stats.path("totalStockQuantity").asLong()).isEqualTo(14);
        assertThat(stats.path("totalOrderValue").decimalValue()).isEqualByComparingTo("65.00");
        var warehouses = read("/api/statistics/warehouses");
        assertThat(warehouses.size()).isEqualTo(3);
        for (int i = 0; i < 3; i++) {
            assertThat(warehouses.get(i).path("warehouseName").asString()).isEqualTo(List.of("Alpha", "Beta", "Empty").get(i));
            assertThat(warehouses.get(i).path("quantity").asLong()).isEqualTo(List.of(10L, 4L, 0L).get(i));
        }
    }

    @Test
    void deliveryAndDeletionRecalculateSummaryWithoutRestockingDeliveredGoods() throws Exception {
        var buyer = user("Buyer", "USER");
        var manager = user("Manager", "MANAGER");
        var product = product("Product", "SKU", category("Category"), "10");
        stock(warehouse("Warehouse"), product, 5, 201);
        var order = id(order(buyer, null, line(product, 2)));
        assertThat(read("/api/statistics").path("activeOrders").asLong()).isEqualTo(1);
        changeStatus(order, manager, "DELIVERED");
        var delivered = read("/api/statistics");
        assertThat(delivered.path("activeOrders").asLong()).isZero();
        assertThat(delivered.path("deliveredOrders").asLong()).isEqualTo(1);
        assertThat(delivered.path("totalStockQuantity").asLong()).isEqualTo(3);
        assertThat(delivered.path("totalOrderValue").decimalValue()).isEqualByComparingTo("20");
        mvc.perform(delete("/api/orders/" + order)).andExpect(status().isNoContent());
        var deleted = read("/api/statistics");
        assertThat(deleted.path("totalOrders").asLong()).isZero();
        assertThat(deleted.path("totalOrderValue").decimalValue()).isEqualByComparingTo("0");
        assertThat(deleted.path("totalStockQuantity").asLong()).isEqualTo(3);
    }
}
