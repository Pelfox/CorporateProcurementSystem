package com.team.corporate.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(@NotNull @Size(max = 72) String currentPassword,
                                     @NotNull @Size(max = 72) String newPassword) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=<redacted>, newPassword=<redacted>]";
    }
}
