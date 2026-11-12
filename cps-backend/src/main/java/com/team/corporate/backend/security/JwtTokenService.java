package com.team.corporate.backend.security;

import com.team.corporate.backend.config.AuthTokenProperties;
import com.team.corporate.core.entities.AuthSession;
import com.team.corporate.core.entities.SessionType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public final class JwtTokenService {
    private final JwtEncoder encoder;
    private final AuthTokenProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder encoder, AuthTokenProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public record IssuedJwt(String value, Instant expiresAt) {
        @Override
        public String toString() {
            return "IssuedJwt[value=<redacted>, expiresAt=" + expiresAt + "]";
        }
    }

    public IssuedJwt issue(AuthSession session) {
        boolean limited = session.getSessionType() == SessionType.PASSWORD_CHANGE;
        Instant now = clock.instant();
        Instant expiry = now.plus(limited ? properties.passwordChangeTtl() : properties.accessTtl());
        if (expiry.isAfter(session.getExpiresAt())) {
            expiry = session.getExpiresAt();
        }
        expiry = expiry.truncatedTo(ChronoUnit.SECONDS);
        if (!expiry.isAfter(now)) {
            throw new org.springframework.security.authentication.BadCredentialsException("Сессия истекла.");
        }
        var claims = JwtClaimsSet.builder().id(java.util.UUID.randomUUID().toString())
                .issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(session.getUser().getId().toString()).issuedAt(now.truncatedTo(ChronoUnit.SECONDS)).expiresAt(expiry)
                .claim("sid", session.getId().toString()).claim("purpose", limited ? "PASSWORD_CHANGE" : "ACCESS").build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        return new IssuedJwt(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiry);
    }
}
