package com.team.corporate.test.backend;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsersHandlerTest extends BackendIntegrationTest {
    @Test
    void createsReadsUpdatesAndDeletesUser() throws Exception {
        UUID manager = user("manager", "MANAGER");
        var created = tree(json(post("/api/users"), fields("username", "  alice  ", "role", "USER"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("alice")).andExpect(jsonPath("$.createdAt").isNotEmpty()));
        UUID id = id(created);
        assertThat(read("/api/users/" + id).path("role").asString()).isEqualTo("USER");
        json(put("/api/users/" + id), fields("username", "Alice Updated", "role", "MANAGER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
        assertThat(jdbc.queryForObject("SELECT username FROM users WHERE id = ?", String.class, id)).isEqualTo("Alice Updated");
        mvc.perform(delete("/api/users/" + id).param("actingUserId", manager.toString())).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/" + id)).andExpect(status().isNotFound());
        assertThat(count("users")).isEqualTo(1);
    }

    @Test
    void filtersByNameIdentifierAndRoleAndSortsBothWays() throws Exception {
        UUID alice = user("Alice", "USER");
        user("Bob", "MANAGER");
        mvc.perform(get("/api/users").param("q", "ALI").param("role", "USER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(alice.toString()));
        mvc.perform(get("/api/users").param("q", alice.toString().substring(0, 8)))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/users").param("sort", "NAME_DESC")).andExpect(jsonPath("$[0].username").value("Bob"));
        mvc.perform(get("/api/users").param("sort", "NAME_ASC")).andExpect(jsonPath("$[0].username").value("Alice"));
        jdbc.update("UPDATE users SET created_at = TIMESTAMP WITH TIME ZONE '2020-01-01 00:00:00+00' WHERE id = ?", alice);
        mvc.perform(get("/api/users").param("sort", "CREATED_ASC")).andExpect(jsonPath("$[0].id").value(alice.toString()));
        mvc.perform(get("/api/users")).andExpect(jsonPath("$[0].username").value("Bob"));
    }

    @Test
    void usernameLookupIsCaseSensitiveTrimsInputAndDoesNotCreateSession() throws Exception {
        UUID alice = user("Alice", "USER");
        var result = mvc.perform(get("/api/users/by-username").param("username", " Alice "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(alice.toString()))
                .andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        mvc.perform(get("/api/users/by-username").param("username", "alice")).andExpect(status().isNotFound());
    }

    @Test
    void duplicateCreateAndEditLeaveStoredUsersUnchanged() throws Exception {
        user("alice", "USER");
        UUID bob = user("bob", "USER");
        json(post("/api/users"), fields("username", "alice", "role", "USER")).andExpect(status().isConflict());
        json(put("/api/users/" + bob), fields("username", "alice", "role", "MANAGER")).andExpect(status().isConflict());
        assertThat(count("users")).isEqualTo(2);
        var stored = read("/api/users/" + bob);
        assertThat(stored.path("username").asString()).isEqualTo("bob");
        assertThat(stored.path("role").asString()).isEqualTo("USER");
    }

    @Test
    void deletionPreservesExistingCoreActorRulesAndForeignKeys() throws Exception {
        UUID manager = user("manager", "MANAGER");
        UUID buyer = user("buyer", "USER");
        mvc.perform(delete("/api/users/" + manager).param("actingUserId", manager.toString())).andExpect(status().isConflict());
        mvc.perform(delete("/api/users/" + manager).param("actingUserId", buyer.toString())).andExpect(status().isConflict());
        mvc.perform(delete("/api/users/" + buyer).param("actingUserId", UUID.randomUUID().toString())).andExpect(status().isNotFound());
        UUID product = product("Paper", "PAPER", category("Office"), "1.00");
        stock(warehouse("Main"), product, 2, 201);
        order(buyer, null, line(product, 1));
        mvc.perform(delete("/api/users/" + buyer).param("actingUserId", manager.toString())).andExpect(status().isConflict());
        assertThat(count("users")).isEqualTo(2);
    }

    @Test
    void unknownUserOperationsReturn404() throws Exception {
        UUID missing = UUID.randomUUID();
        mvc.perform(get("/api/users/" + missing)).andExpect(status().isNotFound());
        json(put("/api/users/" + missing), fields("username", "missing", "role", "USER")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/users/" + missing).param("actingUserId", UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }
}
