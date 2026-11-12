package com.team.corporate.backend.services.responses;

import java.time.Instant;

public record TokenPairResponse(String accessToken, Instant accessExpiresAt,
                                String refreshToken, Instant sessionExpiresAt) {
    @Override
    public String toString() {
        return "TokenPairResponse[accessToken=<redacted>, refreshToken=<redacted>]";
    }
}
