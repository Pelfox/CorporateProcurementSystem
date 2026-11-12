package com.team.corporate.backend.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Component
public final class RefreshTokenCodec {
    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        if (token == null || !FORMAT.matcher(token).matches()) {
            throw new InvalidRefreshTokenException();
        }
        byte[] decoded = Base64.getUrlDecoder().decode(token);
        if (!Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(token)) {
            throw new InvalidRefreshTokenException();
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 недоступен.", impossible);
        }
    }
}
