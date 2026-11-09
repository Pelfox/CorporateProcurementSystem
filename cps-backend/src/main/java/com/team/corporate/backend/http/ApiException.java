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
    public static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "Недостаточно прав для выполнения операции.");
    }

    @NotNull
    public static ApiException forbidden(@NotNull String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    @NotNull
    public static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Требуется аутентификация.");
    }

    @NotNull
    public static ApiException unauthorized(@NotNull String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    @NotNull
    public static ApiException conflict(@NotNull String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    @NotNull
    public static ApiException badRequest(@NotNull String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    @NotNull
    public HttpStatus status() { return status; }
}