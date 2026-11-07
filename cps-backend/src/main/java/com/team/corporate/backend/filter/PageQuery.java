package com.team.corporate.backend.filter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageQuery(@Min(0) Integer page, @Min(1) @Max(100) Integer size) {
    public PageQuery {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }
}
