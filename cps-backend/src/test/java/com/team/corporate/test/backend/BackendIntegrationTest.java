package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BackendMain.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:backend_integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
abstract class BackendIntegrationTest {
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        // Core HibernateTransactions commits independently of Spring test transactions.
        for (String table : List.of("audit_logs", "order_items", "orders", "inventory", "products",
                "warehouses", "categories", "users")) jdbc.update("DELETE FROM " + table);
    }

    protected ResultActions json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    protected JsonNode tree(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsByteArray());
    }

    protected JsonNode read(String path) throws Exception {
        return tree(mvc.perform(get(path)).andExpect(status().isOk()));
    }

    protected UUID user(String name, String role) throws Exception {
        return id(tree(json(post("/api/users"), fields("username", name, "role", role))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))));
    }

    protected UUID category(String name) throws Exception {
        return id(tree(json(post("/api/categories"), fields("name", name)).andExpect(status().isCreated())));
    }

    protected UUID warehouse(String name) throws Exception {
        return id(tree(json(post("/api/warehouses"), fields("name", name, "location", null))
                .andExpect(status().isCreated())));
    }

    protected UUID product(String name, String sku, UUID category, String price) throws Exception {
        return id(tree(json(post("/api/products"), productFields(name, sku, category, price))
                .andExpect(status().isCreated())));
    }

    protected Map<String, Object> productFields(String name, String sku, UUID category, String price) {
        return fields("name", name, "sku", sku, "categoryId", category, "price", new BigDecimal(price));
    }

    protected JsonNode stock(UUID warehouse, UUID product, int quantity, int expected) throws Exception {
        return tree(json(put("/api/warehouses/" + warehouse + "/inventory/" + product), fields("quantity", quantity))
                .andExpect(status().is(expected)));
    }

    @SafeVarargs
    protected final JsonNode order(UUID user, String notes, Map<String, Object>... lines) throws Exception {
        return tree(json(post("/api/orders"), fields("userId", user, "items", List.of(lines), "notes", notes))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")));
    }

    protected JsonNode changeStatus(UUID order, UUID manager, String status) throws Exception {
        return tree(json(patch("/api/orders/" + order + "/status"), fields("managerId", manager, "status", status))
                .andExpect(status().isOk()));
    }

    protected long available(UUID product, UUID editingOrder) throws Exception {
        var request = get("/api/inventory/availability");
        if (editingOrder != null) request.param("editingOrderId", editingOrder.toString());
        for (var row : tree(mvc.perform(request).andExpect(status().isOk()))) {
            if (row.path("productId").asString().equals(product.toString())) return row.path("availableQuantity").asLong();
        }
        throw new AssertionError("Product missing from availability response: " + product);
    }

    protected long physical(UUID product) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(quantity), 0) FROM inventory WHERE product_id = ?", Long.class, product);
    }

    protected long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    protected static UUID id(JsonNode node) { return UUID.fromString(node.path("id").asString()); }

    protected static UUID itemId(JsonNode order, UUID product) {
        for (var item : order.path("items")) {
            if (item.path("productId").asString().equals(product.toString())) return id(item);
        }
        throw new AssertionError("Product missing from order: " + product);
    }

    protected static Map<String, Object> line(UUID product, int quantity) {
        return fields("productId", product, "quantity", quantity);
    }

    protected static Map<String, Object> fields(Object... pairs) {
        var fields = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) fields.put((String) pairs[i], pairs[i + 1]);
        return fields;
    }
}
