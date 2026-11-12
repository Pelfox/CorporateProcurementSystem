package com.team.corporate.backend.validation;

import java.util.Locale;
import java.util.regex.Pattern;

public final class UsernamePolicy {
    private static final Pattern ALLOWED = Pattern.compile("[a-zA-Z0-9._-]{1,64}");

    private UsernamePolicy() {
    }

    public static String normalize(String username) {
        if (username == null) {
            throw new InvalidUsernameException();
        }
        String stripped = username.strip();
        if (!ALLOWED.matcher(stripped).matches()) {
            throw new InvalidUsernameException();
        }
        return stripped.toLowerCase(Locale.ROOT);
    }
}
