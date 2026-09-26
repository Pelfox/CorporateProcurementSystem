package com.team.corporate.utils;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class CsvWriter {
    private CsvWriter() {
    }

    public static void write(Path file, List<String> headers, List<Object[]> rows) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            writeRow(writer, headers.toArray());
            for (Object[] row : rows) {
                writeRow(writer, row);
            }
        }
    }

    private static void writeRow(Writer writer, Object[] values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            Object value = values[i];
            String text = value == null ? "" : value instanceof BigDecimal decimal
                    ? decimal.toPlainString() : value.toString();
            if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\r') >= 0 || text.indexOf('\n') >= 0) {
                writer.write('"');
                writer.write(text.replace("\"", "\"\""));
                writer.write('"');
            } else {
                writer.write(text);
            }
        }
        writer.write("\r\n");
    }
}
