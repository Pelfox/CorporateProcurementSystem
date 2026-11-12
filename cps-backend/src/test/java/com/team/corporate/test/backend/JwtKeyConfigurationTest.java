package com.team.corporate.test.backend;

import com.team.corporate.backend.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtKeyConfigurationTest {
    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withUserConfiguration(JwtConfig.class)
                .withBean(Clock.class, () -> Clock.fixed(AuthTestBeans.NOW, java.time.ZoneOffset.UTC));
    }

    @Test
    void missingKeysFailStartupInsteadOfGeneratingAnEphemeralProductionKey() {
        runner().run(context -> assertThat(context).hasFailed());
    }

    @Test
    void twoIndependentContextsWithTheSameFilesCanVerifyTheSameToken() {
        var runner = runner().withPropertyValues(
                "app.auth.tokens.private-key-location=" + JwtTestKeys.DIRECTORY.resolve("private-key.der").toUri(),
                "app.auth.tokens.public-key-location=" + JwtTestKeys.DIRECTORY.resolve("public-key.der").toUri());
        String subject = UUID.randomUUID().toString();
        var token = new AtomicReference<String>();
        runner.run(context -> {
            var claims = JwtClaimsSet.builder().issuer("corporate-procurement-system").audience(List.of("cps-web"))
                    .subject(subject).claim("sid", UUID.randomUUID().toString()).claim("purpose", "ACCESS")
                    .issuedAt(AuthTestBeans.NOW).expiresAt(AuthTestBeans.NOW.plusSeconds(60)).build();
            token.set(context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(
                    JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue());
        });
        runner.run(context -> assertEquals(subject, context.getBean(JwtDecoder.class).decode(token.get()).getSubject()));
    }

    @Test
    void mismatchedKeyFilesFailStartup(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var generator = java.security.KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var another = generator.generateKeyPair();
        var otherPublicFile = directory.resolve("other-public.der");
        java.nio.file.Files.write(otherPublicFile, another.getPublic().getEncoded());
        runner().withPropertyValues(
                "app.auth.tokens.private-key-location=" + JwtTestKeys.DIRECTORY.resolve("private-key.der").toUri(),
                "app.auth.tokens.public-key-location=" + otherPublicFile.toUri())
                .run(context -> assertThat(context).hasFailed());
    }
}
