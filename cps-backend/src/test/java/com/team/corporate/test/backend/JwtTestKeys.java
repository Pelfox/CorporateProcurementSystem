package com.team.corporate.test.backend;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;

/** Only test JVMs generate disposable keys. Production always loads configured files. */
public final class JwtTestKeys implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    static final Path DIRECTORY = create();

    private static Path create() {
        try {
            Path directory = Files.createTempDirectory("cps-jwt-test-");
            directory.toFile().deleteOnExit();
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            Path privateFile = directory.resolve("private-key.der");
            Path publicFile = directory.resolve("public-key.der");
            Files.write(privateFile, pair.getPrivate().getEncoded());
            Files.write(publicFile, pair.getPublic().getEncoded());
            privateFile.toFile().deleteOnExit();
            publicFile.toFile().deleteOnExit();
            return directory;
        } catch (Exception error) {
            throw new IllegalStateException("Не удалось подготовить тестовые JWT-ключи.", error);
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                "app.auth.tokens.private-key-location=" + DIRECTORY.resolve("private-key.der").toUri(),
                "app.auth.tokens.public-key-location=" + DIRECTORY.resolve("public-key.der").toUri()).applyTo(context);
    }
}
