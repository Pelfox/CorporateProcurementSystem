package com.team.corporate.backend.security;

public class RateLimitException extends RuntimeException {
    private final long retryAfter;

    public RateLimitException(long retryAfter) {
        super("Слишком много запросов. Повторите позже.");
        this.retryAfter = Math.max(1, retryAfter);
    }

    public long retryAfter() {
        return retryAfter;
    }
}
