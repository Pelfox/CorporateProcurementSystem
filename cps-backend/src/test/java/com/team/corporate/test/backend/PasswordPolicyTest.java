package com.team.corporate.test.backend;

import com.team.corporate.backend.security.LocalCompromisedPasswordChecker;
import com.team.corporate.backend.validation.PasswordPolicy;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {
    private final LocalCompromisedPasswordChecker checker = new LocalCompromisedPasswordChecker();
    private final PasswordPolicy policy = new PasswordPolicy(checker);

    @Test
    void permitsPhrasesWhitespaceAndUnicodeWithoutChangingThem() {
        assertDoesNotThrow(() -> policy.validateNewPassword("  A long phrase with spaces  "));
        assertDoesNotThrow(() -> policy.validateNewPassword("Длинная фраза 2026!"));
        assertDoesNotThrow(() -> policy.validateNewPassword("Ab1!".repeat(18)));
        assertDoesNotThrow(() -> policy.validateNewPassword("Пароль-".repeat(4) + "A1!"));
    }

    @Test
    void enforcesCharacterMinimumAndUtf8ByteLimitAndRejectsInvalidText() {
        for (String password : new String[]{"short", "Ab1!".repeat(19), "Ж".repeat(37),
                "Some long phrase\n", "Some long phrase\u0000", "Some long phrase\uD800"}) {
            assertThrows(IllegalArgumentException.class, () -> policy.validateNewPassword(password));
        }
        assertThrows(IllegalArgumentException.class, () -> policy.validateNewPassword(null));
        assertThrows(IllegalArgumentException.class, () -> policy.validateNewPassword("x".repeat(15)));
    }

    @Test
    void checksAnActualBlockedValueEvenWhenItMeetsTheLengthMinimum() throws Exception {
        try (var stream = getClass().getResourceAsStream("/security/common-passwords.txt");
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String blocked = reader.lines().filter(value -> value.length() >= 15 && value.length() <= 72)
                    .findFirst().orElseThrow();
            assertTrue(checker.check(blocked).isCompromised());
            assertThrows(IllegalArgumentException.class, () -> policy.validateNewPassword(blocked));
        }
        assertFalse(checker.check("A separate long phrase 57!").isCompromised());
    }
}
