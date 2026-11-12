package com.team.corporate.backend.filter;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        long totalPages
) {
}
