package com.team.corporate.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefreshRequest(@NotNull @Size(min = 1, max = 4096) String refreshToken) {
    @Override
    public String toString() {
        return "RefreshRequest[refreshToken=<redacted>]";
    }
}
