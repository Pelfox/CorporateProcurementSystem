package com.team.corporate.backend.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthTokenProperties.class)
public class JwtConfig {
    public record SigningKeys(RSAPublicKey publicKey, RSAPrivateKey privateKey, String keyId) {
        @Override
        public String toString() {
            return "SigningKeys[keyId=" + keyId + ", privateKey=<redacted>]";
        }
    }

    @Bean
    SigningKeys signingKeys(AuthTokenProperties properties, ResourceLoader resources) {
        if (properties.privateKeyLocation().isBlank() || properties.publicKeyLocation().isBlank()) {
            throw new IllegalStateException("Задайте JWT_PRIVATE_KEY_LOCATION и JWT_PUBLIC_KEY_LOCATION: внешние DER-файлы PKCS#8/X.509.");
        }
        if (!properties.privateKeyLocation().startsWith("file:") || !properties.publicKeyLocation().startsWith("file:")) {
            throw new IllegalStateException("JWT-ключи должны загружаться из внешних file: ресурсов.");
        }
        try (var privateInput = resources.getResource(properties.privateKeyLocation()).getInputStream();
             var publicInput = resources.getResource(properties.publicKeyLocation()).getInputStream()) {
            var factory = KeyFactory.getInstance("RSA");
            var privateKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(privateInput.readAllBytes()));
            var publicKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(publicInput.readAllBytes()));
            if (publicKey.getModulus().bitLength() < 2048 || !privateKey.getModulus().equals(publicKey.getModulus())) {
                throw new IllegalArgumentException("Нужна согласованная RSA-пара минимум 2048 бит.");
            }
            var signature = java.security.Signature.getInstance("SHA256withRSA");
            byte[] probe = "cps-key-pair-check".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            signature.initSign(privateKey);
            signature.update(probe);
            byte[] signed = signature.sign();
            signature.initVerify(publicKey);
            signature.update(probe);
            if (!signature.verify(signed)) {
                throw new IllegalArgumentException("RSA-ключи не образуют пару.");
            }
            String id = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded()));
            return new SigningKeys(publicKey, privateKey, id);
        } catch (Exception error) {
            throw new IllegalStateException("Не удалось загрузить согласованную RSA-пару для JWT.", error);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(SigningKeys keys) {
        var rsa = new RSAKey.Builder(keys.publicKey()).privateKey(keys.privateKey()).keyID(keys.keyId()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));
    }

    @Bean
    JwtDecoder jwtDecoder(SigningKeys keys, AuthTokenProperties properties, Clock clock) {
        var decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey()).signatureAlgorithm(SignatureAlgorithm.RS256).build();
        var timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp,
                new JwtIssuerValidator(properties.issuer()), jwt -> {
                    try {
                        UUID.fromString(jwt.getSubject());
                        UUID.fromString(jwt.getClaimAsString("sid"));
                        String purpose = jwt.getClaimAsString("purpose");
                        if (!jwt.getAudience().contains(properties.audience()) || jwt.getIssuedAt() == null
                                || jwt.getExpiresAt() == null || jwt.getIssuedAt().isAfter(clock.instant())
                                || !jwt.getExpiresAt().isAfter(clock.instant())
                                || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                                || !("ACCESS".equals(purpose) || "PASSWORD_CHANGE".equals(purpose))) {
                            throw new IllegalArgumentException();
                        }
                        return OAuth2TokenValidatorResult.success();
                    } catch (RuntimeException invalid) {
                        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Недействительный токен.", null));
                    }
                }));
        return decoder;
    }
}
