package com.team.corporate.backend.services.responses;

import java.time.Instant;

public record PasswordChangeTokenResponse(String token, Instant expiresAt) {
    @Override
    public String toString() {
        return "PasswordChangeTokenResponse[token=<redacted>]";
    }
}
