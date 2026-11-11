package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogHandlersTest extends BackendIntegrationTest {
    @Test
    void categoryCrudIncludesProductCountsAndCaseInsensitiveSearch() throws Exception {
        UUID office = category("  Office  ");
        UUID empty = category("Empty");
        product("Paper", "PAPER", office, "1.00");
        assertThat(read("/api/categories/" + office).path("productCount").asLong()).isEqualTo(1);
        assertThat(read("/api/categories/" + empty).path("productCount").asLong()).isZero();
        mvc.perform(get("/api/categories").param("q", "OFF")).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Office"));
        json(put("/api/categories/" + empty), fields("name", "Renamed")).andExpect(status().isOk());
        assertThat(read("/api/categories/" + empty).path("name").asString()).isEqualTo("Renamed");
        mvc.perform(delete("/api/categories/" + empty)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/categories/" + office)).andExpect(status().isConflict());
        assertThat(count("categories")).isEqualTo(1);
    }

    @Test
    void categoryCannotBeDeletedUntilProductIsMoved() throws Exception {
        UUID first = category("First");
        UUID second = category("Second");
        UUID product = product("Paper", "PAPER", first, "1.00");
        mvc.perform(delete("/api/categories/" + first)).andExpect(status().isConflict());
        json(put("/api/products/" + product), productFields("Paper", "PAPER", second, "1.00")).andExpect(status().isOk());
        mvc.perform(delete("/api/categories/" + first)).andExpect(status().isNoContent());
        assertThat(read("/api/categories/" + second).path("productCount").asLong()).isEqualTo(1);
    }

    @Test
    void productCrudPreservesIdAndCategoryAndDeletesUnusedStock() throws Exception {
        UUID first = category("First");
        UUID second = category("Second");
        var created = tree(json(post("/api/products"), productFields("  Paper  ", " PAPER ", first, "0.00"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")));
        UUID product = id(created);
        assertThat(created.path("name").asString()).isEqualTo("Paper");
        assertThat(created.path("sku").asString()).isEqualTo("PAPER");
        assertThat(created.path("price").decimalValue()).isEqualByComparingTo("0.00");
        json(put("/api/products/" + product), productFields("Printer", "PRINT", second, "1234.56"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoryName").value("Second"));
        var stored = read("/api/products/" + product);
        assertThat(stored.path("price").decimalValue()).isEqualByComparingTo("1234.56");
        stock(warehouse("Main"), product, 2, 201);
        mvc.perform(delete("/api/products/" + product)).andExpect(status().isNoContent());
        assertThat(count("products")).isZero();
        assertThat(count("inventory")).isZero();
    }

    @Test
    void productSearchCategoryPriceBoundsAndAllSortModes() throws Exception {
        UUID office = category("Office");
        UUID other = category("Other");
        UUID cheap = product("Alpha", "SKU-A", office, "10.00");
        UUID costly = product("Zulu", "SKU-Z", office, "20.00");
        product("Middle", "SKU-M", other, "15.00");
        mvc.perform(get("/api/products").param("categoryId", office.toString()).param("minPrice", "10").param("maxPrice", "20")
                .param("sort", "PRICE_DESC")).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(costly.toString()));
        mvc.perform(get("/api/products").param("sort", "PRICE_ASC")).andExpect(jsonPath("$[0].id").value(cheap.toString()));
        mvc.perform(get("/api/products").param("sort", "NAME_ASC")).andExpect(jsonPath("$[0].name").value("Alpha"));
        mvc.perform(get("/api/products").param("sort", "NAME_DESC")).andExpect(jsonPath("$[0].name").value("Zulu"));
        for (String query : new String[]{"ALP", "sku-a", cheap.toString().substring(0, 8)}) {
            mvc.perform(get("/api/products").param("q", query)).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(cheap.toString()));
        }
        mvc.perform(get("/api/products").param("q", "absent")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void duplicateSkuAndUnknownCategoryRollBackProductChanges() throws Exception {
        UUID category = category("Office");
        product("First", "FIRST", category, "1.00");
        UUID second = product("Second", "SECOND", category, "2.00");
        json(post("/api/products"), productFields("Duplicate", "FIRST", category, "3.00")).andExpect(status().isConflict());
        json(put("/api/products/" + second), productFields("Changed", "FIRST", category, "3.00"))
                .andExpect(status().isConflict());
        json(put("/api/products/" + second), productFields("Changed", "CHANGED", UUID.randomUUID(), "3.00"))
                .andExpect(status().isNotFound());
        var unchanged = read("/api/products/" + second);
        assertThat(unchanged.path("name").asString()).isEqualTo("Second");
        assertThat(unchanged.path("sku").asString()).isEqualTo("SECOND");
        assertThat(unchanged.path("price").decimalValue()).isEqualByComparingTo("2.00");
        assertThat(count("products")).isEqualTo(2);
    }

    @Test
    void usedProductCannotBeDeletedEvenAfterCancellation() throws Exception {
        UUID category = category("Office");
        UUID product = product("Paper", "PAPER", category, "1.00");
        stock(warehouse("Main"), product, 2, 201);
        var order = order(user("buyer", "USER"), null, line(product, 1));
        changeStatus(id(order), user("manager", "MANAGER"), "CANCELLED");
        mvc.perform(delete("/api/products/" + product)).andExpect(status().isConflict());
        assertThat(count("products")).isEqualTo(1);
        assertThat(physical(product)).isEqualTo(2);
    }

    @Test
    void unknownCatalogResourcesReturn404() throws Exception {
        UUID missing = UUID.randomUUID();
        UUID category = category("Office");
        mvc.perform(get("/api/products/" + missing)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/products/" + missing)).andExpect(status().isNotFound());
        json(put("/api/products/" + missing), productFields("X", "X", category, "1.00")).andExpect(status().isNotFound());
        json(post("/api/products"), productFields("X", "X", missing, "1.00")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products").param("categoryId", missing.toString())).andExpect(status().isNotFound());
        mvc.perform(get("/api/categories/" + missing)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/categories/" + missing)).andExpect(status().isNotFound());
        json(put("/api/categories/" + missing), fields("name", "X")).andExpect(status().isNotFound());
    }
}
