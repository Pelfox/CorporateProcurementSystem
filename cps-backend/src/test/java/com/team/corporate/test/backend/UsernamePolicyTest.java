package com.team.corporate.test.backend;

import com.team.corporate.backend.validation.InvalidUsernameException;
import com.team.corporate.backend.validation.UsernamePolicy;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsernamePolicyTest {
    @Test
    void normalizesUsingRootLocaleAndValidatesBeforeCaseFolding() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("ivan.test-1_2", UsernamePolicy.normalize(" IVAN.test-1_2 "));
            assertEquals("i", UsernamePolicy.normalize("I"));
        } finally {
            Locale.setDefault(previous);
        }
        for (String invalid : new String[]{"", " ", "Иван", "İvan", "with space", "a/b", "a\u0000b", "a".repeat(65)}) {
            assertThrows(InvalidUsernameException.class, () -> UsernamePolicy.normalize(invalid));
        }
        assertThrows(InvalidUsernameException.class, () -> UsernamePolicy.normalize(null));
    }
}
