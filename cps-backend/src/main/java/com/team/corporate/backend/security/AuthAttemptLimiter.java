package com.team.corporate.backend.security;

import com.team.corporate.backend.config.AuthRateLimitProperties;
import com.team.corporate.backend.validation.UsernamePolicy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/** Fixed windows, bounded memory, atomic increments. A successful request does not erase abuse history. */
@Component
public class AuthAttemptLimiter {
    private record Bucket(Instant expiresAt, int count) {
    }

    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Clock clock;
    private final AuthRateLimitProperties properties;

    public AuthAttemptLimiter(Clock clock, AuthRateLimitProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public void login(String username) {
        String normalized;
        try {
            normalized = UsernamePolicy.normalize(username);
        } catch (IllegalArgumentException error) {
            normalized = "<invalid>";
        }
        acquire("login:user:" + normalized, properties.loginUsername());
    }

    public void secret(String action, String value) {
        acquire(action + ":token:" + digest(value), action.equals("refresh") ? properties.refreshToken() : properties.passwordToken());
    }

    public void ip(String action, String address) {
        int limit = switch (action) {
            case "login" -> properties.loginIp();
            case "refresh", "logout" -> properties.refreshIp();
            default -> properties.passwordIp();
        };
        acquire(action + ":ip:" + address, limit);
    }

    public synchronized void acquire(String key, int limit) {
        Instant now = clock.instant();
        buckets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        var bucket = buckets.get(key);
        if (bucket == null) {
            if (buckets.size() >= properties.maxKeys()) {
                // Do not evict live keys: that would let random logins reset another login's counter.
                Instant earliest = buckets.values().stream().map(Bucket::expiresAt).min(Instant::compareTo).orElse(now);
                throw new RateLimitException(seconds(now, earliest));
            }
            buckets.put(key, new Bucket(now.plus(properties.window()), 1));
        } else {
            if (bucket.count() >= limit) {
                throw new RateLimitException(seconds(now, bucket.expiresAt()));
            }
            buckets.put(key, new Bucket(bucket.expiresAt(), bucket.count() + 1));
        }
    }

    private static long seconds(Instant now, Instant until) {
        var remaining = Duration.between(now, until);
        return Math.max(1, remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1));
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 недоступен.", impossible);
        }
    }
}
