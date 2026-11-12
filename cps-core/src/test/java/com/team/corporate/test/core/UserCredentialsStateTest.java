package com.team.corporate.test.core;

import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCredentialsStateTest {
    @Test
    void permanentPasswordClearsTemporaryStateAndResetRestoresIt() {
        var user = new User("buyer", "prepared-temporary-hash", Instant.EPOCH.plusSeconds(100), UserRole.USER);
        user.setPermanentPassword("prepared-permanent-hash");
        assertFalse(user.isMustChangePassword());
        assertNull(user.getTemporaryPasswordExpiresAt());
        user.setEnabled(false);
        user.setTemporaryPassword("prepared-reset-hash", Instant.EPOCH.plusSeconds(200));
        assertTrue(user.isMustChangePassword());
        assertNotNull(user.getTemporaryPasswordExpiresAt());
        assertFalse(user.isEnabled());
        user.setEnabled(true);
        assertTrue(user.isEnabled());
    }

    @Test
    void incompleteCredentialsCannotEnableAnUnprovisionedUser() {
        var user = new User();
        assertFalse(user.isEnabled());
        assertThrows(IllegalStateException.class, () -> user.setEnabled(true));
        assertFalse(user.isEnabled());
    }

    @Test
    void invalidTransitionsLeaveExistingStateIntact() {
        var user = new User("buyer", "prepared-temporary-hash", Instant.EPOCH, UserRole.USER);
        assertThrows(IllegalArgumentException.class, () -> user.setPermanentPassword(" "));
        assertTrue(user.isMustChangePassword());
        assertNotNull(user.getTemporaryPasswordExpiresAt());
        user.setPermanentPassword("prepared-permanent-hash");
        assertThrows(IllegalArgumentException.class, () -> user.setTemporaryPassword("prepared-reset-hash", null));
        assertFalse(user.isMustChangePassword());
        assertNull(user.getTemporaryPasswordExpiresAt());
    }
}
