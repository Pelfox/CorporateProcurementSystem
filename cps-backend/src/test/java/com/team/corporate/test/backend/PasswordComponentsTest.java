package com.team.corporate.test.backend;

import com.team.corporate.backend.config.PasswordConfig;
import com.team.corporate.backend.security.TemporaryPasswordGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordComponentsTest {
    @Test
    void generatedSecretIsBcryptCompatibleAndOnlyMatchesItsOwnHash() {
        var generator = new TemporaryPasswordGenerator();
        var encoder = new PasswordConfig().passwordEncoder(4);
        String secret = generator.generate();
        String hash = encoder.encode(secret);
        assertTrue(secret.length() == 16);
        assertTrue(secret.chars().allMatch(c -> c >= 33 && c <= 126));
        assertTrue(encoder.matches(secret, hash));
        assertFalse(encoder.matches(secret + "x", hash));
        assertFalse(secret.equals(hash));
        assertTrue(hash.startsWith("$2a$04$"));
    }

    @Test
    void passwordGeneratorRejectsUnsupportedLengths() {
        var generator = new TemporaryPasswordGenerator();
        assertThrows(IllegalArgumentException.class, () -> generator.generate(15));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(65));
        assertTrue(generator.generate(64).length() == 64);
    }
}
