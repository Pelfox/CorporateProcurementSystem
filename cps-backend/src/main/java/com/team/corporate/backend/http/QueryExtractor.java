package com.team.corporate.backend.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class QueryExtractor {
    private QueryExtractor() {
    }

    @NotNull
    public static String normalize(@Nullable  String query) {
        return query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    }

    public static boolean contains(@Nullable String value, @NotNull String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }
}
