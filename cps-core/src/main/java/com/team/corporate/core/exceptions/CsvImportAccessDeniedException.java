package com.team.corporate.core.exceptions;

public class CsvImportAccessDeniedException extends RuntimeException {
    public CsvImportAccessDeniedException(String message) {
        super(message);
    }
}
