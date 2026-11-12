package com.team.corporate.test.backend;

import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.backend.services.responses.TokenPairResponse;
import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.services.CategoriesService;
import com.team.corporate.core.services.ProductsService;
import com.team.corporate.core.services.WarehousesService;
import com.team.corporate.core.services.InventoriesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = true)
class BusinessAuthorizationHttpTest extends AuthLifecycleTestBase {
    @Autowired private ApiExecutor api;
    @Autowired private CategoriesService categories;
    @Autowired private ProductsService products;
    @Autowired private WarehousesService warehouses;
    @Autowired private InventoriesService inventory;

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, TokenPairResponse pair) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + pair.accessToken());
    }

    private UUID fixtureProduct() {
        return api.execute(() -> {
            var category = categories.createCategory("Secure category");
            var product = products.createProduct("Secure product", "secure-sku", category.getId(), new BigDecimal("100.00"));
            var warehouse = warehouses.createWarehouse("Secure warehouse", null);
            inventory.placeProductInWarehouse(product.getId(), warehouse.getId(), 50);
            return product.getId();
        });
    }

    @Test
    void userCanCreateReadAndEditOwnCreatedOrderButNeverForeignObjectsOrActors() throws Exception {
        UUID product = fixtureProduct();
        var alice = activate("order_alice");
        var bob = activate("order_bob");
        UUID aliceId = users.findByUsername("order_alice").orElseThrow().getId();
        UUID bobId = users.findByUsername("order_bob").orElseThrow().getId();
        var own = tree(json(as(post("/api/orders"), alice), fields("userId", aliceId, "items", List.of(line(product, 2)), "notes", "own"))
                .andExpect(status().isCreated()));
        UUID id = id(own);
        UUID item = itemId(own, product);
        json(as(post("/api/orders"), bob), fields("userId", bobId, "items", List.of(line(product, 1)), "notes", "foreign"))
                .andExpect(status().isCreated());
        mvc.perform(as(get("/api/orders"), alice)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(aliceId.toString()));
        mvc.perform(as(get("/api/orders").param("userId", bobId.toString()), alice)).andExpect(status().isForbidden());
        for (String suffix : new String[]{"", "/items", "/audit"}) {
            mvc.perform(as(get("/api/orders/" + id + suffix), bob)).andExpect(status().isForbidden());
        }
        mvc.perform(as(get("/api/inventory/availability").param("editingOrderId", id.toString()), bob)).andExpect(status().isForbidden());
        json(as(post("/api/orders"), alice), fields("userId", bobId, "items", List.of(line(product, 1))))
                .andExpect(status().isForbidden());
        json(as(patch("/api/orders/" + id + "/notes"), bob), fields("userId", aliceId, "notes", "spoofed"))
                .andExpect(status().isForbidden());
        json(as(patch("/api/orders/" + id + "/notes"), alice), fields("userId", aliceId, "notes", "updated"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notes").value("updated"));
        json(as(put("/api/orders/" + id + "/items/" + item), alice), fields("userId", aliceId, "quantity", 3))
                .andExpect(status().isOk());
        mvc.perform(as(delete("/api/orders/" + id), alice)).andExpect(status().isForbidden());
        mvc.perform(as(delete("/api/orders/" + id + "/items/" + item).param("userId", aliceId.toString()), bob))
                .andExpect(status().isForbidden());
        json(as(patch("/api/orders/" + id + "/status"), alice), fields("managerId", aliceId, "status", "APPROVED"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"MANAGER", "ADMIN"})
    void procurementManagersCanManageAllStatusesButCannotSpoofActorOrBypassCreatedState(UserRole role) throws Exception {
        UUID product = fixtureProduct();
        var owner = activate("status_owner");
        var manager = activate("status_manager");
        jdbc.update("UPDATE users SET user_role = ? WHERE username = 'status_manager'", role.name());
        UUID ownerId = users.findByUsername("status_owner").orElseThrow().getId();
        UUID managerId = users.findByUsername("status_manager").orElseThrow().getId();
        UUID id = id(tree(json(as(post("/api/orders"), owner), fields("userId", ownerId, "items", List.of(line(product, 2))))
                .andExpect(status().isCreated())));
        mvc.perform(as(get("/api/orders/" + id), manager)).andExpect(status().isOk());
        json(as(patch("/api/orders/" + id + "/status"), manager), fields("managerId", ownerId, "status", "APPROVED"))
                .andExpect(status().isForbidden());
        json(as(patch("/api/orders/" + id + "/status"), manager), fields("managerId", managerId, "status", "APPROVED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        json(as(patch("/api/orders/" + id + "/notes"), owner), fields("userId", ownerId, "notes", "too late"))
                .andExpect(status().isConflict());
        mvc.perform(as(get("/api/exports/csv").param("managerId", ownerId.toString()), manager)).andExpect(status().isForbidden());
        if (role == UserRole.ADMIN) {
            mvc.perform(as(delete("/api/admin/users/" + ownerId), manager)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Пользователь связан с заказами или аудитом. Используйте блокировку."));
        }
        mvc.perform(as(delete("/api/orders/" + id), manager)).andExpect(status().isNoContent());
    }

    @Test
    void businessAuditPreventsDeletingAManagerWithoutOwnOrders() throws Exception {
        UUID product = fixtureProduct();
        var owner = activate("audit_owner");
        var manager = activate("audit_manager");
        var admin = activate("audit_admin");
        jdbc.update("UPDATE users SET user_role = 'MANAGER' WHERE username = 'audit_manager'");
        jdbc.update("UPDATE users SET user_role = 'ADMIN' WHERE username = 'audit_admin'");
        UUID ownerId = users.findByUsername("audit_owner").orElseThrow().getId();
        UUID managerId = users.findByUsername("audit_manager").orElseThrow().getId();
        UUID order = id(tree(json(as(post("/api/orders"), owner), fields("userId", ownerId, "items", List.of(line(product, 1))))
                .andExpect(status().isCreated())));
        json(as(patch("/api/orders/" + order + "/status"), manager), fields("managerId", managerId, "status", "APPROVED"))
                .andExpect(status().isOk());
        mvc.perform(as(delete("/api/admin/users/" + managerId), admin)).andExpect(status().isConflict());
    }
}
