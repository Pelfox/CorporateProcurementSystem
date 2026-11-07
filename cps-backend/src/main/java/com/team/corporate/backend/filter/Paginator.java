package com.team.corporate.backend.filter;

import java.util.List;

public final class Paginator {
    private Paginator() {
    }

    public static <T> PageResponse<T> paginate(List<T> results, PageQuery query) {
        int page = query.page();
        int size = query.size();
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Номер страницы должен быть от 0, размер — от 1.");
        }

        int totalElements = results.size();
        long offset = (long) page * size;
        int from = (int) Math.min(offset, totalElements);
        int to = (int) Math.min(offset + size, totalElements);

        return new PageResponse<>(List.copyOf(results.subList(from, to)), page, size,
                totalElements, Math.ceilDiv(totalElements, size));
    }
}
