package com.team.corporate.core.dto;

import java.util.UUID;

public record CategoryProductCountResponse(
        UUID categoryId,
        String categoryName,
        long count
) {
}
