package com.team.corporate.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotNull @Size(max = 256) String username, @NotNull @Size(max = 72) String password) {
    @Override
    public String toString() {
        return "LoginRequest[credentials=<redacted>]";
    }
}
