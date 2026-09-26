package com.team.corporate;

import com.team.corporate.utils.CsvWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvWriterTest {
    @TempDir
    Path directory;

    @Test
    void preservesUnicodeAndEscapesCsvFields() throws Exception {
        Path file = directory.resolve("data.csv");
        CsvWriter.write(file, List.of("text", "price", "empty"), List.<Object[]>of(
                new Object[]{"Бумага, \"А4\"\r\nВторая строка", new BigDecimal("1234.50"), null},
                new Object[]{"Строка\nс переносом", new BigDecimal("1E+3"), ""},
                new Object[]{"Возврат\rкаретки", BigDecimal.ZERO, "обычный текст"}));

        assertEquals("text,price,empty\r\n"
                        + "\"Бумага, \"\"А4\"\"\r\nВторая строка\",1234.50,\r\n"
                        + "\"Строка\nс переносом\",1000,\r\n"
                        + "\"Возврат\rкаретки\",0,обычный текст\r\n",
                Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void emptyTableStillHasHeaders() throws Exception {
        Path file = directory.resolve("empty.csv");
        CsvWriter.write(file, List.of("id", "name"), List.of());
        assertEquals("id,name\r\n", Files.readString(file));
    }

    @Test
    void refusesToOverwriteExistingFile() throws Exception {
        Path file = directory.resolve("existing.csv");
        Files.writeString(file, "original");
        assertThrows(FileAlreadyExistsException.class,
                () -> CsvWriter.write(file, List.of("id"), List.of()));
        assertEquals("original", Files.readString(file));
    }
}
