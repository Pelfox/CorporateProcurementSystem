package com.team.corporate.backend.validation;

import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 15;
    public static final int MAX_UTF8_BYTES = 72;
    private final CompromisedPasswordChecker compromisedPasswords;

    public PasswordPolicy(CompromisedPasswordChecker compromisedPasswords) {
        this.compromisedPasswords = compromisedPasswords;
    }

    public void validateNewPassword(String password) {
        if (!isBcryptCompatible(password)
                || password.codePointCount(0, password.length()) < MIN_LENGTH) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 15 символов и не более 72 UTF-8 байт, без управляющих символов.");
        }
        if (password.codePoints().distinct().limit(2).count() < 2
                || compromisedPasswords.check(password).isCompromised()) {
            throw new IllegalArgumentException("Выберите пароль, отсутствующий в списке распространённых слабых паролей.");
        }
    }

    public static boolean isBcryptCompatible(String password) {
        return password != null && !password.isEmpty() && password.length() <= MAX_UTF8_BYTES
                && password.codePoints().noneMatch(Character::isISOControl)
                && StandardCharsets.UTF_8.newEncoder().canEncode(password)
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES;
    }
}
