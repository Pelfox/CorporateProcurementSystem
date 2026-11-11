package com.team.corporate.backend.http;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(@NotNull HttpStatus status, @NotNull String message) {
        super(message);
        this.status = status;
    }

    @NotNull
    public static ApiException notFound(@NotNull String resource) {
        return new ApiException(HttpStatus.NOT_FOUND, resource + " не найден.");
    }

    @NotNull
    public HttpStatus status() {
        return status;
    }
}
