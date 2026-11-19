package com.team.corporate.core.dto;

import java.util.List;
import java.util.UUID;

public record CsvValidationResponse(
        UUID importId,
        int totalRows,
        int validRows,
        int invalidRows,
        boolean canApply,
        List<CsvRowError> errors
) {
}
