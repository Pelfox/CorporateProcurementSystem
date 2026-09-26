package com.team.corporate.utils;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

public final class CsvWriter {
    private CsvWriter() {
    }

    public static void write(Path file, List<String> headers, List<Object[]> rows) throws IOException {
        EntityValidation.requireNonNull(file, "Путь к CSV-файлу");
        EntityValidation.requireNonNull(headers, "Заголовки CSV");
        EntityValidation.requireNonNull(rows, "Строки CSV");
        if (file.toString().isBlank() || headers.isEmpty()) {
            throw new IllegalArgumentException("Укажите путь к файлу и хотя бы один столбец CSV.");
        }
        var names = new HashSet<String>();
        for (String header : headers) {
            if (header == null || header.isBlank() || !names.add(header.strip())) {
                throw new IllegalArgumentException("Заголовки CSV должны быть непустыми и уникальными.");
            }
        }

        var prepared = new ArrayList<String[]>();
        prepared.add(prepareRow(headers.toArray()));
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            if (row == null || row.length != headers.size()) {
                throw new IllegalArgumentException("Строка CSV " + (i + 1)
                        + ": количество значений должно совпадать с количеством столбцов (" + headers.size() + ").");
            }
            prepared.add(prepareRow(row));
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            for (String[] row : prepared) {
                writeRow(writer, row);
            }
        }
    }

    private static String[] prepareRow(Object[] values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value instanceof Double d && !Double.isFinite(d)
                    || value instanceof Float f && !Float.isFinite(f)) {
                throw new IllegalArgumentException("CSV не поддерживает NaN и бесконечные числа.");
            }
            String text = value == null ? "" : value instanceof BigDecimal decimal
                    ? decimal.toPlainString() : value.toString();
            validateText(text);
            result[i] = !(value instanceof Number) && isFormula(text) ? "'" + text : text;
        }
        return result;
    }

    private static void validateText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c) && c != '\t' && c != '\r' && c != '\n') {
                throw new IllegalArgumentException("CSV содержит недопустимый управляющий символ.");
            }
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                    i++;
                    continue;
                }
                throw new IllegalArgumentException("CSV содержит некорректный Unicode.");
            }
            if (Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("CSV содержит некорректный Unicode.");
            }
        }
    }

    private static boolean isFormula(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\t' || c == '\r' || c == '\n') {
                return true;
            }
            if (Character.isWhitespace(c) || Character.isSpaceChar(c) || Character.getType(c) == Character.FORMAT) {
                continue;
            }
            return "=+-@＝＋－＠".indexOf(c) >= 0;
        }
        return false;
    }

    private static void writeRow(Writer writer, String[] values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            String text = values[i];
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
