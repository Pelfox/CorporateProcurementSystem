package com.team.corporate.backend.security;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/** Offline common-password snapshot; it does not query or represent every public breach. */
@Component
public final class LocalCompromisedPasswordChecker implements CompromisedPasswordChecker {
    private final Set<String> blocked;

    public LocalCompromisedPasswordChecker() {
        var resource = new ClassPathResource("security/common-passwords.txt");
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            blocked = reader.lines().filter(line -> !line.isEmpty()).collect(Collectors.toUnmodifiableSet());
        } catch (IOException error) {
            throw new IllegalStateException("Не удалось загрузить локальный список слабых паролей.", error);
        }
        if (blocked.isEmpty()) {
            throw new IllegalStateException("Локальный список слабых паролей пуст.");
        }
    }

    @Override
    public CompromisedPasswordDecision check(String password) {
        return new CompromisedPasswordDecision(blocked.contains(password));
    }
}
