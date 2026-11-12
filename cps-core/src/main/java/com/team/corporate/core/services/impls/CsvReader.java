package com.team.corporate.core.services.impls;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class CsvReader implements Closeable {
    private final PushbackReader source;
    private boolean first = true;

    CsvReader(InputStream input) {
        source = new PushbackReader(new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder())));
    }

    List<String> readRecord() throws IOException {
        int ch = source.read();
        if (first) {
            first = false;
            if (ch == '\uFEFF') {
                ch = source.read();
            }
        }
        if (ch == -1) {
            return null;
        }

        var fields = new ArrayList<String>();
        do {
            var field = new StringBuilder();
            ch = ch == '"' ? readQuoted(field) : readUnquoted(ch, field);
            fields.add(field.toString());
            if (ch != ',') {
                break;
            }
            ch = source.read();
        } while (true);

        if (ch == '\r') {
            int next = source.read();
            if (next != '\n' && next != -1) {
                source.unread(next);
            }
        }
        return fields;
    }

    private int readQuoted(StringBuilder field) throws IOException {
        while (true) {
            int ch = source.read();
            if (ch == -1) {
                throw new IllegalArgumentException("Не закрыто поле в кавычках.");
            }
            if (ch == '"') {
                int next = source.read();
                if (next != '"') {
                    if (!isSeparator(next)) {
                        throw new IllegalArgumentException("После закрывающей кавычки ожидалась запятая или конец записи.");
                    }
                    return next;
                }
            }
            field.append((char) ch);
        }
    }

    private int readUnquoted(int ch, StringBuilder field) throws IOException {
        while (!isSeparator(ch)) {
            if (ch == '"') {
                throw new IllegalArgumentException("Кавычка внутри поля должна быть экранирована.");
            }
            field.append((char) ch);
            ch = source.read();
        }
        return ch;
    }

    private static boolean isSeparator(int ch) {
        return ch == ',' || ch == '\r' || ch == '\n' || ch == -1;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
