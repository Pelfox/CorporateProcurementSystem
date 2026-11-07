package com.team.corporate.backend.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {
    }
}
