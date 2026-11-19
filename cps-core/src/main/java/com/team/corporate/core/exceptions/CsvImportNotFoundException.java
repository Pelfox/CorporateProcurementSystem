package com.team.corporate.core.exceptions;

public class CsvImportNotFoundException extends RuntimeException {
    public CsvImportNotFoundException(String message) {
        super(message);
    }
}
