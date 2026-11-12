package com.team.corporate.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InitialPasswordRequest(@NotNull @Size(max = 72) String newPassword) {
    @Override
    public String toString() {
        return "InitialPasswordRequest[newPassword=<redacted>]";
    }
}
