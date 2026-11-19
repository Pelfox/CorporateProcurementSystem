package com.team.corporate.backend.controllers;

import com.team.corporate.core.dto.CsvApplyResponse;
import com.team.corporate.core.dto.CsvValidationResponse;
import com.team.corporate.core.dto.ImportMode;
import com.team.corporate.core.services.CsvImportService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/imports")
@PreAuthorize("hasRole('MANAGER')")
public class CsvImportController {
    private final CsvImportService importService;

    public CsvImportController(CsvImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/products/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvValidationResponse validateProducts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "mode", defaultValue = "CREATE_ONLY") ImportMode mode
    ) throws IOException {
        return importService.validateProducts(UUID.fromString(jwt.getSubject()), file.getBytes(), mode);
    }

    @PostMapping("/products/{importId}/apply")
    public CsvApplyResponse applyProducts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("importId") UUID importId
    ) {
        return importService.applyProducts(UUID.fromString(jwt.getSubject()), importId);
    }
}
