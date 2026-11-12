package com.team.corporate.backend.handlers;

import com.team.corporate.core.services.CsvExportService;
import com.team.corporate.backend.security.BusinessAccess;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/exports")
public class ExportsHandler {
    private final CsvExportService exporter;
    private final BusinessAccess access;

    public ExportsHandler(@NotNull CsvExportService exporter, BusinessAccess access) {
        this.exporter = exporter;
        this.access = access;
    }

    @GetMapping(value = "/csv", produces = "application/zip")
    public ResponseEntity<byte[]> csv(@RequestParam(required = false) UUID managerId) throws IOException {
        UUID actorId = access.actor(managerId);
        var temporary = Files.createTempDirectory("cps-http-export-");
        try {
            var directory = exporter.exportAll(actorId, temporary);
            var output = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(output); var files = Files.list(directory)) {
                for (var file : files.sorted().toList()) {
                    zip.putNextEntry(new ZipEntry(file.getFileName().toString()));
                    Files.copy(file, zip);
                    zip.closeEntry();
                }
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename("corporate-procurement.zip").build().toString())
                    .body(output.toByteArray());
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
