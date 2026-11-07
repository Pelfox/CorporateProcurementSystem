package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RequestValidationTest extends BackendIntegrationTest {
    private static final String ID = "00000000-0000-0000-0000-000000000001";
    private static final String ORDER = "/api/orders/" + ID;
    private static final String ITEM = ORDER + "/items/" + ID;
    private static final String STOCK = "/api/warehouses/" + ID + "/inventory/" + ID;

    record Endpoint(String method, String path, Map<String, Object> body) {
        MockHttpServletRequestBuilder request() {
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path);
        }
    }

    static Stream<Endpoint> bodies() {
        return Stream.of(
                new Endpoint("POST", "/api/users", fields("username", "User", "role", "USER")),
                new Endpoint("PUT", "/api/users/" + ID, fields("username", "User", "role", "USER")),
                new Endpoint("POST", "/api/categories", fields("name", "Category")),
                new Endpoint("PUT", "/api/categories/" + ID, fields("name", "Category")),
                new Endpoint("POST", "/api/warehouses", fields("name", "Warehouse", "location", null)),
                new Endpoint("PUT", "/api/warehouses/" + ID, fields("name", "Warehouse", "location", null)),
                new Endpoint("POST", "/api/products", fields("name", "Product", "sku", "SKU", "categoryId", ID, "price", 1)),
                new Endpoint("PUT", "/api/products/" + ID, fields("name", "Product", "sku", "SKU", "categoryId", ID, "price", 1)),
                new Endpoint("PUT", STOCK, fields("quantity", 0)),
                new Endpoint("POST", "/api/orders", fields("userId", ID, "items", List.of(fields("productId", ID, "quantity", 1)))),
                new Endpoint("PUT", ORDER, fields("userId", ID, "items", List.of(fields("productId", ID, "quantity", 1)))),
                new Endpoint("PATCH", ORDER + "/notes", fields("userId", ID, "notes", null)),
                new Endpoint("POST", ORDER + "/items", fields("userId", ID, "productId", ID, "quantity", 1)),
                new Endpoint("PUT", ITEM, fields("userId", ID, "quantity", 1)),
                new Endpoint("PATCH", ORDER + "/status", fields("managerId", ID, "status", "APPROVED")));
    }

    @ParameterizedTest(name = "{0}: empty object")
    @MethodSource("bodies")
    void everyBodyIsValidatedBeforeCallingServices(Endpoint endpoint) throws Exception {
        json(endpoint.request(), Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isNotEmpty()).andExpect(jsonPath("$.path").value(endpoint.path()));
        assertThat(count("orders") + count("users") + count("products")).isZero();
    }

    @ParameterizedTest(name = "{0}: missing body")
    @MethodSource("bodies")
    void everyBodyIsRequired(Endpoint endpoint) throws Exception {
        mvc.perform(endpoint.request().contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}: unknown property")
    @MethodSource("bodies")
    void everyBodyRejectsUnknownProperties(Endpoint endpoint) throws Exception {
        var body = new LinkedHashMap<>(endpoint.body());
        body.put("unexpected", "value");
        json(endpoint.request(), body).andExpect(status().isBadRequest());
    }

    static Stream<Endpoint> invalidFields() {
        var cases = new ArrayList<Endpoint>();
        bodies().forEach(endpoint -> endpoint.body().forEach((field, valid) -> {
            var invalid = new ArrayList<Object>();
            switch (field) {
                case "name", "sku", "username" -> invalid.addAll(Arrays.asList(null, "", " \t ", "x".repeat(256), "a\u0001b"));
                case "notes", "location" -> invalid.addAll(List.of("x".repeat(256), "a\u0001b"));
                case "userId", "managerId", "categoryId", "productId" -> invalid.addAll(Arrays.asList(null, "bad-uuid"));
                case "role", "status" -> invalid.addAll(Arrays.asList(null, "UNKNOWN", 0));
                case "price" -> invalid.addAll(Arrays.asList(null, -1, "100000000", "0.001", "NaN"));
                case "quantity" -> {
                    invalid.addAll(Arrays.asList(null, -1, 1.5, 2147483648L));
                    if (!endpoint.path().equals(STOCK)) invalid.addAll(List.of(0, 100001));
                }
                case "items" -> invalid.addAll(Arrays.asList(null, List.of(), Collections.singletonList(null),
                        List.of(fields("quantity", 1)), List.of(fields("productId", ID)),
                        List.of(fields("productId", ID, "quantity", 0)), List.of(fields("productId", ID, "quantity", 100001)),
                        List.of(fields("productId", ID, "quantity", 1.5)), List.of(fields("productId", "bad-uuid", "quantity", 1))));
                default -> throw new AssertionError(field);
            }
            for (Object value : invalid) {
                var body = new LinkedHashMap<>(endpoint.body());
                body.put(field, value);
                cases.add(new Endpoint(endpoint.method(), endpoint.path(), body));
            }
        }));
        return cases.stream();
    }

    @ParameterizedTest(name = "invalid body {index}: {0}")
    @MethodSource("invalidFields")
    void rejectsInvalidFieldsIncludingNestedItems(Endpoint endpoint) throws Exception {
        json(endpoint.request(), endpoint.body()).andExpect(status().isBadRequest());
    }

    record Query(String path, Map<String, String> params) { }

    static Stream<Query> invalidQueries() {
        var cases = new ArrayList<Query>();
        for (String path : List.of("/api/users", "/api/categories", "/api/products", "/api/warehouses", "/api/orders")) {
            for (String bad : List.of("x".repeat(256), "a\u0001b")) cases.add(new Query(path, Map.of("q", bad)));
        }
        for (String bad : List.of("", " ", "x".repeat(256), "a\u0001b"))
            cases.add(new Query("/api/users/by-username", Map.of("username", bad)));
        cases.add(new Query("/api/users/by-username", Map.of()));
        cases.add(new Query("/api/products", Map.of("minPrice", "2", "maxPrice", "1")));
        cases.add(new Query("/api/orders", Map.of("minTotal", "2", "maxTotal", "1")));
        cases.add(new Query("/api/orders", Map.of("dateFrom", "2026-02-02", "dateTo", "2026-02-01")));
        for (String key : List.of("minPrice", "maxPrice")) for (String bad : List.of("-1", "bad"))
            cases.add(new Query("/api/products", Map.of(key, bad)));
        for (String key : List.of("minTotal", "maxTotal")) for (String bad : List.of("-1", "bad"))
            cases.add(new Query("/api/orders", Map.of(key, bad)));
        for (String key : List.of("dateFrom", "dateTo")) for (String bad : List.of("2026-02-30", "bad"))
            cases.add(new Query("/api/orders", Map.of(key, bad)));
        for (String path : List.of("/api/users", "/api/products", "/api/orders"))
            cases.add(new Query(path, Map.of("sort", "UNKNOWN")));
        cases.add(new Query("/api/users", Map.of("role", "UNKNOWN")));
        cases.add(new Query("/api/orders", Map.of("status", "UNKNOWN")));
        for (String key : List.of("productId", "warehouseId")) cases.add(new Query("/api/inventory", Map.of(key, "bad")));
        cases.add(new Query("/api/inventory/availability", Map.of("editingOrderId", "bad")));
        cases.add(new Query("/api/products", Map.of("categoryId", "bad")));
        cases.add(new Query("/api/orders", Map.of("userId", "bad")));
        return cases.stream();
    }

    @ParameterizedTest(name = "invalid query {index}: {0}")
    @MethodSource("invalidQueries")
    void validatesAllQueryDtosAndTypedParameters(Query query) throws Exception {
        var request = get(query.path());
        query.params().forEach(request::param);
        mvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    void requiredQueryIdsRejectMissingBlankAndMalformedValues() throws Exception {
        for (String value : Arrays.asList(null, "", "bad")) {
            var userDelete = delete("/api/users/" + ID);
            var itemDelete = delete(ITEM);
            var export = get("/api/exports/csv");
            if (value != null) {
                userDelete.param("actingUserId", value);
                itemDelete.param("userId", value);
                export.param("managerId", value);
            }
            mvc.perform(userDelete).andExpect(status().isBadRequest());
            mvc.perform(itemDelete).andExpect(status().isBadRequest());
            mvc.perform(export).andExpect(status().isBadRequest());
        }
    }

    @Test
    void malformedPathIdsAreRejectedBeforeServices() throws Exception {
        for (String path : List.of("/api/users/bad", "/api/categories/bad", "/api/products/bad", "/api/warehouses/bad",
                "/api/inventory/bad", "/api/orders/bad", "/api/orders/bad/items", "/api/orders/bad/audit",
                "/api/warehouses/bad/inventory")) mvc.perform(get(path)).andExpect(status().isBadRequest());
        json(put("/api/warehouses/bad/inventory/" + ID), fields("quantity", 1)).andExpect(status().isBadRequest());
        json(put("/api/warehouses/" + ID + "/inventory/bad"), fields("quantity", 1)).andExpect(status().isBadRequest());
        json(put(ORDER + "/items/bad"), fields("userId", ID, "quantity", 1)).andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonAndWrongMediaTypeProduceClientErrors() throws Exception {
        for (String body : List.of("{", "null", "[]"))
            mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/users").contentType(MediaType.TEXT_PLAIN).content("username=User"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(content().contentType(MediaType.APPLICATION_JSON));
        mvc.perform(patch("/api/products/" + ID)).andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow")).andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void validationErrorsIdentifyNestedFieldAndKeepDatabaseUntouched() throws Exception {
        var error = tree(json(post("/api/orders"), fields("userId", ID,
                "items", List.of(fields("productId", ID, "quantity", 0)))).andExpect(status().isBadRequest()));
        assertThat(error.path("violations").get(0).path("field").asString()).isEqualTo("items[0].quantity");
        assertThat(error.path("timestamp").asString()).isNotBlank();
        assertThat(count("orders")).isZero();
        assertThat(count("order_items")).isZero();
    }

    @Test
    void acceptsTextAndNumericBoundariesAndOptionalValues() throws Exception {
        var category = category("x".repeat(255));
        var product = product("Товар\nс описанием", "SKU\t1", category, "99999999.99");
        var warehouse = warehouse("Warehouse");
        stock(warehouse, product, 100000, 201);
        var buyer = user("x".repeat(255), "USER");
        var order = order(buyer, null, line(product, 100000));
        assertThat(order.path("totalQuantity").asInt()).isEqualTo(100000);
        assertThat(order.path("notes").isNull()).isTrue();
        json(put("/api/products/" + product), productFields("Product", "SKU", category, "0.00")).andExpect(status().isOk());
        mvc.perform(get("/api/products").param("minPrice", "0").param("maxPrice", "0")).andExpect(status().isOk());
        mvc.perform(get("/api/orders").param("dateFrom", "2026-01-01").param("dateTo", "2026-01-01")
                .param("minTotal", "0").param("maxTotal", "0")).andExpect(status().isOk());
    }
}
