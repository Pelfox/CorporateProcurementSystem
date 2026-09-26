package com.team.corporate;

import com.team.corporate.utils.CsvWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Arrays;

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

    @ParameterizedTest
    @ValueSource(strings = {"=1+1", "+1+1", "-1+1", "@SUM(A1)", "  =1", "\t=1", "\r=1", "\n=1",
            "\u00a0=1", "\ufeff=1", "＝1+1", "＋1", "－1", "＠SUM(A1)"})
    void neutralizesFormulaPrefixesInHeadersAndValues(String value) throws Exception {
        Path file = directory.resolve("safe.csv");
        CsvWriter.write(file, List.of(value), List.<Object[]>of(new Object[]{value}));
        String protectedValue = "'" + value;
        if (value.contains("\r") || value.contains("\n")) {
            protectedValue = "\"" + protectedValue + "\"";
        }
        assertEquals(protectedValue + "\r\n" + protectedValue + "\r\n", Files.readString(file));
    }

    @Test
    void preservesNumbersAndOrdinaryText() throws Exception {
        Path file = directory.resolve("values.csv");
        CsvWriter.write(file, List.of("number", "text", "unicode"), List.<Object[]>of(
                new Object[]{new BigDecimal("-12.50"), "A+B@example.com", "Товар 📦"}));
        assertEquals("number,text,unicode\r\n-12.50,A+B@example.com,Товар 📦\r\n", Files.readString(file));
    }

    @Test
    void rejectsInvalidStructureBeforeCreatingFile() {
        Path file = directory.resolve("invalid.csv");
        assertThrows(IllegalArgumentException.class, () -> CsvWriter.write(null, List.of("id"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> CsvWriter.write(file, null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> CsvWriter.write(file, List.of("id"), null));
        for (List<String> headers : List.of(List.<String>of(), List.of(" "), List.of("id", "id"),
                List.of("id", " id "), Arrays.asList("id", null))) {
            assertThrows(IllegalArgumentException.class, () -> CsvWriter.write(file, headers, List.of()));
        }
        for (Object[] badRow : Arrays.asList(null, new Object[]{}, new Object[]{1, 2})) {
            assertThrows(IllegalArgumentException.class, () -> CsvWriter.write(file, List.of("id"),
                    Arrays.asList(new Object[]{1}, badRow)));
        }
        assertFalse(Files.exists(file));
    }

    @Test
    void rejectsInvalidCellsWithoutLeavingPartialFile() {
        Path file = directory.resolve("invalid.csv");
        for (Object value : List.of("bad\u0000text", "bad\u001btext", "\ud800", "\udc00",
                Double.NaN, Double.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertThrows(IllegalArgumentException.class, () -> CsvWriter.write(file, List.of("value"),
                    List.of(new Object[]{"valid first row"}, new Object[]{value})));
            assertFalse(Files.exists(file));
        }
    }
}
