package com.team.corporate.backend.controllers;

import com.team.corporate.core.services.CsvExportService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/exports")
@PreAuthorize("hasRole('MANAGER')")
public class CsvExportController {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CsvExportController.class);

    private final CsvExportService exportService;

    public CsvExportController(CsvExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping(value = "/all", produces = "application/zip")
    public ResponseEntity<byte @NotNull []> exportAllEntities(@AuthenticationPrincipal Jwt jwt) throws IOException {
        UUID managerId = UUID.fromString(jwt.getSubject());

        Path temporaryDirectory = Files.createTempDirectory("cps-export-");

        try {
            Path csvDirectory = exportService.exportAll(managerId, temporaryDirectory);

            byte[] archive = createZip(csvDirectory);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.zip\"")
                    .body(archive);
        } finally {
            try {
                FileSystemUtils.deleteRecursively(temporaryDirectory);
            } catch (IOException exception) {
                LOGGER.warn(
                        "Не удалось удалить временную папку экспорта: {}",
                        temporaryDirectory,
                        exception
                );
            }
        }
    }

    private byte[] createZip(Path directory) throws IOException {
        var buffer = new ByteArrayOutputStream();

        try (var zip = new ZipOutputStream(buffer);
             var files = Files.list(directory)) {

            for (Path file : files.sorted().toList()) {
                zip.putNextEntry(
                        new ZipEntry(file.getFileName().toString())
                );

                Files.copy(file, zip);
                zip.closeEntry();
            }
        }

        return buffer.toByteArray();
    }
}
