package com.team.corporate.test.backend;

import com.team.corporate.backend.security.InvalidRefreshTokenException;
import com.team.corporate.backend.security.RefreshTokenCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenCodecTest {
    @Test
    void uses32RandomBytesAndOnlyHashesCanonicalRepresentations() {
        var codec = new RefreshTokenCodec();
        String raw = codec.generate();
        assertEquals(32, java.util.Base64.getUrlDecoder().decode(raw).length);
        assertTrue(codec.hash(raw).matches("[0-9a-f]{64}"));
        assertEquals(codec.hash(raw), codec.hash(raw));
        assertThrows(InvalidRefreshTokenException.class, () -> codec.hash(null));
        assertThrows(InvalidRefreshTokenException.class, () -> codec.hash("invalid"));
        assertThrows(InvalidRefreshTokenException.class, () -> codec.hash(raw + "="));
    }
}
