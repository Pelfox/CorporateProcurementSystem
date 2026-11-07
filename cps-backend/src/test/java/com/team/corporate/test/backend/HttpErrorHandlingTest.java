package com.team.corporate.test.backend;

import com.team.corporate.backend.BackendMain;
import com.team.corporate.backend.handlers.ExportsHandler;
import com.team.corporate.backend.handlers.UsersHandler;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.core.services.CsvExportService;
import com.team.corporate.core.services.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {UsersHandler.class, ExportsHandler.class})
@ContextConfiguration(classes = BackendMain.class)
class HttpErrorHandlingTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private UsersService users;
    @MockitoBean private ApiExecutor api;
    @MockitoBean private CsvExportService exporter;

    @BeforeEach
    void executeHandlerActions() {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(api).execute(any());
    }

    @Test
    void unexpectedFailureReturnsStableJsonWithoutInternalDetails() throws Exception {
        when(users.getAll()).thenThrow(new IllegalStateException("internal-host secret SQL SELECT * FROM users"));
        var response = mvc.perform(get("/api/users")).andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value(500)).andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Не удалось выполнить операцию."))
                .andExpect(jsonPath("$.path").value("/api/users")).andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.violations").isEmpty()).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("internal-host", "secret", "SELECT", "IllegalStateException", "stackTrace");
    }

    @Test
    void databaseConstraintFailureReturnsConflictWithoutSqlDetails() throws Exception {
        when(users.getAll()).thenThrow(new IllegalArgumentException("unique index internal_users_key",
                new SQLException("INSERT INTO users secret", "23505")));
        var response = mvc.perform(get("/api/users")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Операция конфликтует с существующими данными."))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("internal_users_key", "INSERT", "secret", "23505");
    }

    @Test
    void exportIoFailureReturnsJsonAndRemovesPartialTemporaryFiles() throws Exception {
        var temporary = new AtomicReference<Path>();
        when(exporter.exportAll(any(UUID.class), any(Path.class))).thenAnswer(invocation -> {
            Path root = invocation.getArgument(1);
            temporary.set(root);
            var directory = Files.createDirectory(root.resolve("partial-export"));
            Files.writeString(directory.resolve("users.csv"), "partial contents");
            throw new IOException("private filesystem path and disk failure");
        });
        var response = mvc.perform(get("/api/exports/csv").param("managerId", UUID.randomUUID().toString())
                        .accept("application/zip"))
                .andExpect(status().isInternalServerError()).andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.path").value("/api/exports/csv"))
                .andExpect(jsonPath("$.message").value("Не удалось выполнить операцию."))
                .andExpect(header().doesNotExist("Content-Disposition")).andReturn().getResponse().getContentAsString();
        assertThat(temporary.get()).isNotNull().doesNotExist();
        assertThat(response).doesNotContain("private filesystem", "disk failure", "partial contents");
    }

    @Test
    void successfulArchiveAlsoRemovesTemporaryDirectory() throws Exception {
        var temporary = new AtomicReference<Path>();
        when(exporter.exportAll(any(UUID.class), any(Path.class))).thenAnswer(invocation -> {
            Path root = invocation.getArgument(1);
            temporary.set(root);
            var directory = Files.createDirectory(root.resolve("complete-export"));
            Files.writeString(directory.resolve("users.csv"), "id,username\r\n");
            return directory;
        });
        mvc.perform(get("/api/exports/csv").param("managerId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType("application/zip"));
        assertThat(temporary.get()).isNotNull().doesNotExist();
    }

    @Test
    void unknownRouteReturnsJsonNotAnInternalError() throws Exception {
        mvc.perform(get("/api/does-not-exist")).andExpect(status().isNotFound())
                .andExpect(content().contentType("application/json")).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"));
    }
}
