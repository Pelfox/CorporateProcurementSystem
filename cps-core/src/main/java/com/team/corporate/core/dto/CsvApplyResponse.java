package com.team.corporate.core.dto;

import java.util.UUID;

public record CsvApplyResponse(
        UUID importId,
        int createdRows,
        int updatedRows
) {
}
