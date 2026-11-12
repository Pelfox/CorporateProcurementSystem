package com.team.corporate.core.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface CsvImportService {
    Result importFiles(List<Source> files) throws IOException;

    record Source(String filename, StreamOpener content) {
    }

    @FunctionalInterface
    interface StreamOpener {
        InputStream open() throws IOException;
    }

    record Result(Map<String, Integer> imported, int totalImported) {
    }
}
