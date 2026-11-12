package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImportsHandlerTest extends LegacyBusinessIntegrationTest {
    private static final String PATH = "/api/imports/csv";

    @Test
    void restoresAllExportedTablesFromFilesInReverseOrder() throws Exception {
        var manager = user("manager", "MANAGER");
        var buyer = user("buyer", "USER");
        var category = category("Канц, \"бумага\"");
        String productName = "=" + "x".repeat(254);
        var product = product(productName, "SKU", category, "12.30");
        var warehouse = id(tree(json(post("/api/warehouses"),
                fields("name", "Склад", "location", "Дом 1\nЭтаж 2")).andExpect(status().isCreated())));
        stock(warehouse, product, 5, 201);
        var order = order(buyer, "строка 1\r\nстрока \"2\", текст", line(product, 2));
        changeStatus(id(order), manager, "APPROVED");
        var exported = export(manager);
        cleanDatabase();

        var request = multipart(PATH);
        exported.reversed().forEach((name, csv) -> request.file(file(name, csv)));
        mvc.perform(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalImported").value(9))
                .andExpect(jsonPath("$.imported['users.csv']").value(2))
                .andExpect(jsonPath("$.imported['audit_logs.csv']").value(1));

        assertThat(export(manager)).isEqualTo(exported);
        assertThat(read("/api/products/" + product).path("name").asString()).isEqualTo(productName);
        assertThat(available(product, null)).isEqualTo(3);
    }

    @Test
    void acceptsBomAndLeavesHeaderOnlyFilesEmpty() throws Exception {
        var category = UUID.randomUUID();
        mvc.perform(multipart(PATH)
                        .file(file("categories.csv", "\uFEFFid,name\n" + category + ",Канцтовары"))
                        .file(file("warehouses.csv", "id,name,location\r\n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalImported").value(1))
                .andExpect(jsonPath("$.imported['warehouses.csv']").value(0));
        assertThat(read("/api/categories/" + category).path("name").asString()).isEqualTo("Канцтовары");
    }

    @Test
    void rollsBackEarlierRowsAndFilesWhenAProductConflicts() throws Exception {
        var existing = product("Existing", "TAKEN", category("Existing"), "1.00");
        var newCategory = UUID.randomUUID();
        String products = "id,sku,name,category_id,price\n"
                + UUID.randomUUID() + ",NEW,New," + newCategory + ",2.00\n"
                + UUID.randomUUID() + ",TAKEN,Duplicate," + newCategory + ",3.00";

        mvc.perform(multipart(PATH).file(file("products.csv", products))
                        .file(file("categories.csv", "id,name\n" + newCategory + ",New")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("products.csv, запись 3")));

        assertThat(count("categories")).isEqualTo(1);
        assertThat(count("products")).isEqualTo(1);
        assertThat(read("/api/products/" + existing).path("name").asString()).isEqualTo("Existing");
    }

    @Test
    void rejectsMissingReferencesAndReservationsAboveStock() throws Exception {
        String missing = UUID.randomUUID() + ",NEW,New," + UUID.randomUUID() + ",2.00";
        mvc.perform(multipart(PATH).file(file("products.csv", "id,sku,name,category_id,price\n" + missing)))
                .andExpect(status().isConflict());
        assertThat(count("products")).isZero();

        var product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 5, 201);
        var order = id(order(user("Buyer", "USER"), null, line(product, 2)));
        String extra = UUID.randomUUID() + "," + order + "," + product + ",4,1.00";
        mvc.perform(multipart(PATH).file(file("order_items.csv", "id,order_id,product_id,quantity,purchase_price\n" + extra)))
                .andExpect(status().isConflict());
        assertThat(count("order_items")).isEqualTo(1);
        assertThat(available(product, null)).isEqualTo(3);
    }

    @Test
    void rejectsMalformedFilesWithoutWriting() throws Exception {
        mvc.perform(multipart(PATH)).andExpect(status().isBadRequest());
        for (var invalid : new MockMultipartFile[]{
                file("unknown.csv", "id,name\n"),
                file("categories.csv", "name,id\n"),
                file("categories.csv", "id,name\n" + UUID.randomUUID() + ",\"Unclosed"),
                file("categories.csv", "id,name\ninvalid,Name"),
                new MockMultipartFile("file", "categories.csv", "text/csv", new byte[]{(byte) 0xC3})
        }) {
            mvc.perform(multipart(PATH).file(invalid)).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart(PATH).file(file("categories.csv", "id,name\n"))
                        .file(file("categories.csv", "id,name\n")))
                .andExpect(status().isBadRequest());
        assertThat(count("categories")).isZero();
    }

    private SequencedMap<String, String> export(UUID manager) throws Exception {
        byte[] body = mvc.perform(get("/api/exports/csv").param("managerId", manager.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        var files = new LinkedHashMap<String, String>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(body), StandardCharsets.UTF_8)) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                files.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return files;
    }

    private static MockMultipartFile file(String name, String csv) {
        return new MockMultipartFile("file", name, "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }
}
