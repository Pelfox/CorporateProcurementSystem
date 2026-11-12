package com.team.corporate.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.auth.tokens")
public record AuthTokenProperties(
        @DefaultValue("corporate-procurement-system") String issuer,
        @DefaultValue("cps-web") String audience,
        @DefaultValue("PT10M") Duration accessTtl,
        @DefaultValue("PT5M") Duration passwordChangeTtl,
        @DefaultValue("P7D") Duration sessionTtl,
        @DefaultValue("P1D") Duration historyRetention,
        @DefaultValue("") String privateKeyLocation,
        @DefaultValue("") String publicKeyLocation) {

    public AuthTokenProperties {
        if (issuer.isBlank() || audience.isBlank()) {
            throw new IllegalArgumentException("Issuer и audience должны быть заданы.");
        }
        for (Duration ttl : new Duration[]{accessTtl, passwordChangeTtl, sessionTtl}) {
            if (ttl.isNegative() || ttl.isZero() || ttl.getNano() != 0) {
                throw new IllegalArgumentException("Сроки токенов задаются положительным целым числом секунд.");
            }
        }
        if (historyRetention.isNegative()) {
            throw new IllegalArgumentException("Срок хранения истории не может быть отрицательным.");
        }
    }
}
