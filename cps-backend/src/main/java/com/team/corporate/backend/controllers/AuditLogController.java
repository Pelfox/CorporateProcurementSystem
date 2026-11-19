package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.AuditLogResponse;
import com.team.corporate.backend.dto.CreateAuditLogRequest;
import com.team.corporate.backend.search.AuditLogFilter;
import com.team.corporate.backend.services.AuditLogsSearchService;
import com.team.corporate.core.services.AuditLogsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
    private final AuditLogsService audit;
    private final AuditLogsSearchService search;

    public AuditLogController(AuditLogsService audit, AuditLogsSearchService search) {
        this.audit = audit;
        this.search = search;
    }

    @GetMapping
    public PagedModel<AuditLogResponse> findAll(
            @Valid @ModelAttribute AuditLogFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Sort sort
    ) {
        return new PagedModel<>(search.findAll(filter, page, size, sort));
    }

    @PostMapping
    public ResponseEntity<AuditLogResponse> createAuditLog(
            @Valid @RequestBody CreateAuditLogRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var log = audit.createAuditLog(request.orderId(), UUID.fromString(jwt.getSubject()),
                request.oldStatus(), request.newStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuditLogResponse.of(log));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuditLog(@PathVariable("id") UUID id) {
        audit.deleteAuditLog(id);
    }
}
