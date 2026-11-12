package com.team.corporate.core.exceptions;

public class CsvImportException extends RuntimeException {
    public enum Kind {
        INVALID_CSV,
        CONFLICT
    }

    private final Kind kind;

    public CsvImportException(Kind kind, String message) {
        this(kind, message, null);
    }

    public CsvImportException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
