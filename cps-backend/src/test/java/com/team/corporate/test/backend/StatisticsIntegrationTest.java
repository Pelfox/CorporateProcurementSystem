package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(classes = BackendMain.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:backend-statistics;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "cps.security.bootstrap.username=", "cps.security.bootstrap.password=",
        "cps.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class StatisticsIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void analyticsEndpointsHandleAnEmptyDatabase() throws Exception {
        var products = statistics("/products", "MANAGER");
        assertEquals(0, products.get("total").asLong());
        assertTrue(products.get("averagePrice").isNull());
        assertEquals(0, statistics("/products/by-category", "MANAGER").size());
        var statuses = statistics("/orders/by-status", "MANAGER");
        assertEquals(4, statuses.size());
        for (var status : statuses) assertEquals(0, status.get("count").asLong());
        var levels = statistics("/products/by-stock", "MANAGER");
        assertEquals(3, levels.size());
        for (var level : levels) assertEquals(0, level.get("count").asLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void statisticsEndpointsReadPersistedData(String role) throws Exception {
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        jdbc.update("INSERT INTO categories (id, name, version) VALUES (?, 'Stationery', 0)", categoryId);
        jdbc.update("INSERT INTO products (id, sku, name, category_id, price, version) VALUES (?, 'PAPER', 'Paper', ?, 12.34, 0)",
                productId, categoryId);
        jdbc.update("INSERT INTO products (id, sku, name, price, deleted_at, version) VALUES (?, 'OLD', 'Archived', 99.99, CURRENT_TIMESTAMP, 0)",
                UUID.randomUUID());
        jdbc.update("INSERT INTO warehouses (id, name, version) VALUES (?, 'Main', 0)", warehouseId);
        jdbc.update("INSERT INTO inventory (id, product_id, warehouse_id, quantity, version) VALUES (?, ?, ?, 6, 0)",
                UUID.randomUUID(), productId, warehouseId);

        var products = statistics("/products", role);
        assertEquals(2, products.get("total").asLong());
        assertEquals(1, products.get("active").asLong());
        assertEquals(1, products.get("archived").asLong());
        assertEquals(0, products.get("withTags").asLong());
        assertEquals(0, new BigDecimal("12.34").compareTo(products.get("averagePrice").decimalValue()));

        var categories = statistics("/products/by-category", role);
        assertEquals(1, categories.size());
        assertEquals(categoryId.toString(), categories.get(0).get("categoryId").asString());
        assertEquals("Stationery", categories.get(0).get("categoryName").asString());
        assertEquals(1, categories.get(0).get("count").asLong());

        var statuses = statistics("/orders/by-status", role);
        assertEquals(4, statuses.size());
        for (var status : statuses) assertEquals(0, status.get("count").asLong());

        var levels = statistics("/products/by-stock", role);
        assertEquals(3, levels.size());
        for (var level : levels) {
            assertEquals("IN_STOCK".equals(level.get("level").asString()) ? 1 : 0,
                    level.get("count").asLong());
        }

        var summary = statistics("/summary", role);
        assertEquals(1, summary.get("totalProducts").asLong());
        assertEquals(6, summary.get("totalStockQuantity").asLong());
    }

    private JsonNode statistics(String path, String role) throws Exception {
        var result = mvc.perform(get("/api/statistics" + path).with(user("statistics-reader").roles(role)))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return JSON.readTree(result.getResponse().getContentAsString());
    }
}
