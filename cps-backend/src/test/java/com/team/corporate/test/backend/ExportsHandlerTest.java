package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExportsHandlerTest extends BackendIntegrationTest {
    private static final Map<String, String> HEADERS = Map.of(
            "users.csv", "id,username,user_role,created_at",
            "categories.csv", "id,name",
            "products.csv", "id,sku,name,category_id,price",
            "warehouses.csv", "id,name,location",
            "inventories.csv", "id,product_id,warehouse_id,quantity",
            "orders.csv", "id,user_id,order_status,notes,created_at,updated_at",
            "order_items.csv", "id,order_id,product_id,quantity,purchase_price",
            "audit_logs.csv", "id,order_id,changed_by,old_status,new_status,changed_at");

    private byte[] export(UUID manager) throws Exception {
        return mvc.perform(get("/api/exports/csv").param("managerId", manager.toString()))
                .andExpect(status().isOk()).andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"corporate-procurement.zip\""))
                .andReturn().getResponse().getContentAsByteArray();
    }

    private static Map<String, String> unzip(byte[] bytes) throws Exception {
        var files = new LinkedHashMap<String, String>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                assertThat(entry.isDirectory()).isFalse();
                assertThat(files.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8))).isNull();
            }
        }
        return files;
    }

    @Test
    void archiveContainsExactlyEightTablesAndHeadersForEmptyTables() throws Exception {
        var files = unzip(export(user("Manager", "MANAGER")));
        assertThat(files.keySet()).containsExactlyInAnyOrderElementsOf(HEADERS.keySet());
        HEADERS.forEach((name, header) -> assertThat(files.get(name)).startsWith(header + "\r\n"));
        for (String name : HEADERS.keySet()) if (!name.equals("users.csv"))
            assertThat(files.get(name)).isEqualTo(HEADERS.get(name) + "\r\n");
        assertThat(rows(files.get("users.csv"))).hasSize(2);
    }

    @Test
    void archivePreservesRelationshipsPricesUnicodeAndCsvEscaping() throws Exception {
        var manager = user("Менеджер", "MANAGER");
        var buyer = user("Иван", "USER");
        var category = category("Канц, \"бумага\"");
        var product = product("=SUM(1,2)", "SKU", category, "12.30");
        var warehouse = id(tree(json(post("/api/warehouses"), fields("name", "Склад", "location", "Дом 1\nЭтаж 2"))
                .andExpect(status().isCreated())));
        var inventory = id(stock(warehouse, product, 5, 201));
        var order = order(buyer, "строка 1\nстрока \"2\", текст", line(product, 2));
        changeStatus(id(order), manager, "APPROVED");
        json(put("/api/products/" + product), productFields("=SUM(1,2)", "SKU", category, "99.99"))
                .andExpect(status().isOk());
        var files = unzip(export(manager));

        assertThat(files.get("categories.csv")).contains(category + ",\"Канц, \"\"бумага\"\"\"\r\n");
        assertThat(files.get("products.csv")).contains(product + ",SKU,\"'=SUM(1,2)\"," + category + ",99.99\r\n");
        assertThat(files.get("warehouses.csv")).contains(warehouse + ",Склад,\"Дом 1\nЭтаж 2\"\r\n");
        assertThat(files.get("orders.csv")).contains(",APPROVED,\"строка 1\nстрока \"\"2\"\", текст\",");
        assertThat(rows(files.get("inventories.csv")).get(1)).containsExactly(inventory.toString(), product.toString(), warehouse.toString(), "5");
        assertThat(rows(files.get("order_items.csv")).get(1)).containsExactly(itemId(order, product).toString(), id(order).toString(), product.toString(), "2", "12.30");
        var audit = rows(files.get("audit_logs.csv")).get(1);
        assertThat(audit.subList(1, 5)).containsExactly(id(order).toString(), manager.toString(), "CREATED", "APPROVED");
        assertThat(rows(files.get("users.csv"))).anySatisfy(row -> assertThat(row.subList(0, 3)).containsExactly(buyer.toString(), "Иван", "USER"));
    }

    @Test
    void repeatedExportsAreReadOnlyAndEarlierArchiveRemainsASnapshot() throws Exception {
        var manager = user("Manager", "MANAGER");
        var category = category("Before");
        byte[] first = export(manager);
        assertThat(unzip(export(manager))).isEqualTo(unzip(first));
        json(put("/api/categories/" + category), fields("name", "After")).andExpect(status().isOk());
        assertThat(unzip(first).get("categories.csv")).contains(category + ",Before\r\n").doesNotContain("After");
        assertThat(unzip(export(manager)).get("categories.csv")).contains(category + ",After\r\n");
        assertThat(count("users")).isEqualTo(1);
        assertThat(count("categories")).isEqualTo(1);
        assertThat(count("orders") + count("audit_logs") + count("order_items")).isZero();
    }

    @Test
    void exportUsesExplicitManagerIdAndExistingServiceRoleRules() throws Exception {
        var buyer = user("Buyer", "USER");
        for (var actor : List.of(buyer, UUID.randomUUID())) {
            mvc.perform(get("/api/exports/csv").param("managerId", actor.toString()).accept("application/zip"))
                    .andExpect(status().isConflict()).andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.status").value(409)).andExpect(header().doesNotExist("Set-Cookie"));
        }
        assertThat(count("users")).isEqualTo(1);
    }

    private static List<List<String>> rows(String csv) {
        var rows = new ArrayList<List<String>>();
        var row = new ArrayList<String>();
        var field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') { field.append('"'); i++; }
                else quoted = !quoted;
            } else if (!quoted && ch == ',') { row.add(field.toString()); field.setLength(0); }
            else if (!quoted && ch == '\n') {
                row.add(field.toString()); rows.add(List.copyOf(row)); row.clear(); field.setLength(0);
            } else if (quoted || ch != '\r') field.append(ch);
        }
        assertThat(quoted).as("Unclosed CSV quote").isFalse();
        assertThat(field).isEmpty();
        assertThat(row).isEmpty();
        return rows;
    }
}
