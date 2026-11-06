package com.team.corporate.backend.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public final class TemporaryPasswordGenerator {
    private static final int DEFAULT_LENGTH = 16;
    private static final int MIN_LENGTH = 16;
    private static final int MAX_LENGTH = 64;

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String PUNCTUATION = "!@#$%^&*()-_=+[]{}|;:,.<>?";
    private static final String ALPHABET = LOWER + UPPER + DIGITS + PUNCTUATION;

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public static String generate(int length) {
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Длина пароля должна быть от "
                            + MIN_LENGTH + " до "
                            + MAX_LENGTH + " символов."
            );
        }

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            password.append(ALPHABET.charAt(index));
        }
        return password.toString();
    }
}
