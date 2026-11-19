package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(classes = BackendMain.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:backend-web-workflow;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "cps.security.bootstrap.username=", "cps.security.bootstrap.password=",
        "cps.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class WebWorkflowIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    private RequestPostProcessor actor(UUID id, String role) {
        return jwt().jwt(token -> token.subject(id.toString())).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void categoriesCanBeSearchedWithoutAGetBodyAndDeleted() throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO categories (id, name, version) VALUES (?, 'Office', 0)", id);
        var reader = actor(UUID.randomUUID(), "USER");
        var found = mvc.perform(get("/api/categories").param("name", "off").with(reader)).andReturn();
        assertEquals(200, found.getResponse().getStatus(), found.getResponse().getContentAsString());
        assertEquals(1, JSON.readTree(found.getResponse().getContentAsString()).get("content").size());
        assertEquals(204, mvc.perform(delete("/api/categories/" + id).with(reader)).andReturn().getResponse().getStatus());
        entityManager.flush();
        assertNotNull(jdbc.queryForObject("SELECT deleted_at FROM categories WHERE id = ?", Object.class, id));
    }

    @Test
    void productsWithoutCategoriesRemainVisibleInTheCatalogueAndAnalytics() throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO products (id, sku, name, price, version) VALUES (?, 'UNCATEGORIZED', 'Paper', 12.34, 0)", id);
        var manager = actor(UUID.randomUUID(), "MANAGER");
        var catalogue = mvc.perform(get("/api/products").with(manager)).andReturn();
        assertEquals(200, catalogue.getResponse().getStatus());
        assertTrue(JSON.readTree(catalogue.getResponse().getContentAsString()).get("content").get(0).get("categoryId").isNull());
        var groups = mvc.perform(get("/api/statistics/products/by-category").with(manager)).andReturn();
        assertEquals(200, groups.getResponse().getStatus(), groups.getResponse().getContentAsString());
        JsonNode category = JSON.readTree(groups.getResponse().getContentAsString()).get(0);
        assertTrue(category.get("categoryId").isNull());
        assertEquals("Без категории", category.get("categoryName").asString());
        assertEquals(1, category.get("count").asInt());
    }

    @Test
    void checkoutAndEditingSaveTheWholeCartAndRejectInsufficientStockAtomically() throws Exception {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, username, full_name, user_role, enabled, created_at, version) VALUES (?, 'buyer', 'Buyer', 'USER', true, CURRENT_TIMESTAMP, 0)", buyerId);
        jdbc.update("INSERT INTO products (id, sku, name, price, version) VALUES (?, 'PAPER', 'Paper', 12.34, 0)", productId);
        jdbc.update("INSERT INTO warehouses (id, name, version) VALUES (?, 'Main', 0)", warehouseId);
        jdbc.update("INSERT INTO inventory (id, product_id, warehouse_id, quantity, version) VALUES (?, ?, ?, 6, 0)", UUID.randomUUID(), productId, warehouseId);
        var buyer = actor(buyerId, "USER");
        var body = Map.of("items", List.of(Map.of("productId", productId, "quantity", 2)), "notes", "First order");
        var created = mvc.perform(post("/api/orders/checkout").with(buyer).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body))).andReturn();
        assertEquals(201, created.getResponse().getStatus(), created.getResponse().getContentAsString());
        JsonNode order = JSON.readTree(created.getResponse().getContentAsString());
        String orderId = order.get("id").asString();
        assertEquals(buyerId.toString(), order.get("userId").asString());
        assertEquals("CREATED", order.get("status").asString());
        assertEquals(24.68, order.get("totalSum").asDouble(), 0.001);
        var contents = Map.of("items", List.of(Map.of("productId", productId, "quantity", 3)), "notes", "Updated");
        var saved = mvc.perform(put("/api/orders/" + orderId + "/contents").with(buyer).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(contents))).andReturn();
        assertEquals(200, saved.getResponse().getStatus(), saved.getResponse().getContentAsString());
        assertEquals(37.02, JSON.readTree(saved.getResponse().getContentAsString()).get("totalSum").asDouble(), 0.001);
        var availability = mvc.perform(get("/api/orders/availability").param("editingOrderId", orderId).with(buyer)).andReturn();
        assertEquals(200, availability.getResponse().getStatus());
        assertEquals(6, JSON.readTree(availability.getResponse().getContentAsString()).get(productId.toString()).asLong());
        assertEquals(403, mvc.perform(get("/api/orders/availability").param("editingOrderId", orderId).with(actor(UUID.randomUUID(), "USER"))).andReturn().getResponse().getStatus());
        var insufficient = Map.of("items", List.of(Map.of("productId", productId, "quantity", 100)));
        assertEquals(400, mvc.perform(post("/api/orders/checkout").with(buyer).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(insufficient))).andReturn().getResponse().getStatus());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE deleted_at IS NULL", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT quantity FROM order_items WHERE deleted_at IS NULL", Integer.class));
    }
}
