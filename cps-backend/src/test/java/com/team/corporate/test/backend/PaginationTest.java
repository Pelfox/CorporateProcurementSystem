package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaginationTest extends BackendIntegrationTest {
    @Test
    void allListRoutesReturnTheSameOrderWhenPaginated() throws Exception {
        var office = category("Office");
        category("Other");
        var paper = product("Paper", "PAPER", office, "10.00");
        var pen = product("Pen", "PEN", office, "20.00");
        var main = warehouse("Main");
        warehouse("Other");
        stock(main, paper, 10, 201);
        stock(main, pen, 10, 201);

        var buyer = user("buyer", "USER");
        var manager = user("manager", "MANAGER");
        var orderId = id(order(buyer, null, line(paper, 1), line(pen, 1)));
        changeStatus(orderId, manager, "APPROVED");
        changeStatus(orderId, manager, "CANCELLED");
        order(buyer, null, line(paper, 1));

        for (String path : List.of(
                "/api/products",
                "/api/categories",
                "/api/warehouses",
                "/api/users",
                "/api/orders",
                "/api/orders/" + orderId + "/items",
                "/api/orders/" + orderId + "/audit",
                "/api/inventory",
                "/api/inventory/availability",
                "/api/warehouses/" + main + "/inventory",
                "/api/statistics/warehouses"
        )) {
            var list = read(path);
            var page = read(path + "/page?page=1&size=1");
            assertThat(list.size()).as(path).isEqualTo(2);
            assertThat(page.path("items").size()).as(path).isEqualTo(1);
            assertThat(page.path("items").get(0)).as(path).isEqualTo(list.get(1));
            assertThat(page.path("page").asInt()).as(path).isEqualTo(1);
            assertThat(page.path("size").asInt()).as(path).isEqualTo(1);
            assertThat(page.path("totalElements").asLong()).as(path).isEqualTo(2);
            assertThat(page.path("totalPages").asLong()).as(path).isEqualTo(2);
        }
    }

    @Test
    void productFiltersAndSortApplyBeforePagination() throws Exception {
        var office = category("Office");
        product("Paper", "PAPER", office, "10.00");
        var pen = product("Pen", "PEN", office, "20.00");
        product("Pencil", "PENCIL", office, "30.00");
        product("Box", "BOX", office, "15.00");

        mvc.perform(get("/api/products/page").param("categoryId", office.toString())
                        .param("q", "P").param("maxPrice", "25").param("sort", "PRICE_DESC").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(pen.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void paginationHandlesEmptyResultsAndLargeOffsetsAndValidatesParameters() throws Exception {
        mvc.perform(get("/api/products/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(0));

        product("Paper", "PAPER", category("Office"), "10.00");
        mvc.perform(get("/api/products/page").param("page", String.valueOf(Integer.MAX_VALUE)).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        mvc.perform(get("/api/products/page").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/page").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/page").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/page").param("size", "oops")).andExpect(status().isBadRequest());
    }
}
