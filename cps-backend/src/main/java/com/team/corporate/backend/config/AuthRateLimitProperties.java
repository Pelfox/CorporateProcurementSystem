package com.team.corporate.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("app.auth.rate-limit")
public record AuthRateLimitProperties(
        @DefaultValue("PT1M") Duration window,
        @DefaultValue("120") int loginIp,
        @DefaultValue("10") int loginUsername,
        @DefaultValue("120") int refreshIp,
        @DefaultValue("30") int refreshToken,
        @DefaultValue("60") int passwordIp,
        @DefaultValue("10") int passwordToken,
        @DefaultValue("10000") int maxKeys,
        @DefaultValue("4") int maxParallelPasswordChecks,
        @DefaultValue List<String> trustedProxies) {
    public AuthRateLimitProperties {
        if (window.isNegative() || window.isZero() || window.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Окно limiter должно быть от 1 нс до суток.");
        }
        for (int limit : new int[]{loginIp, loginUsername, refreshIp, refreshToken, passwordIp, passwordToken,
                maxKeys, maxParallelPasswordChecks}) {
            if (limit < 1) {
                throw new IllegalArgumentException("Пределы limiter должны быть положительными.");
            }
        }
        trustedProxies = List.copyOf(trustedProxies);
    }
}
