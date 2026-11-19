package com.team.corporate.core.dto;

public record CsvRowError(
        int row,
        String field,
        String message
) {
}
