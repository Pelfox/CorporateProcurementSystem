package com.team.corporate.console;

import org.jetbrains.annotations.NotNull;
import org.jline.reader.LineReader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

public final class ConsoleInput {
    private final LineReader reader;

    public ConsoleInput(@NotNull LineReader reader) {
        this.reader = reader;
    }

    public void print(@NotNull String text) {
        reader.getTerminal().writer().println(text);
        reader.getTerminal().flush();
    }

    public String text(@NotNull String prompt) {
        return reader.readLine(prompt + ": ").trim();
    }

    public String required(@NotNull String prompt) {
        while (true) {
            String value = text(prompt);
            if (!value.isBlank()) {
                return value;
            }
            print("Значение не должно быть пустым.");
        }
    }

    public int number(@NotNull String prompt, int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(text(prompt));
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            print("Введите целое число от " + min + " до " + max + ".");
        }
    }

    @NotNull
    public BigDecimal money(@NotNull String prompt) {
        while (true) {
            try {
                BigDecimal value = new BigDecimal(text(prompt).replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY);
                if (value.signum() >= 0 && value.precision() <= 10) {
                    return value;
                }
            } catch (IllegalArgumentException | ArithmeticException ignored) {
            }
            print("Введите цену от 0 до 99999999,99, не более двух знаков после запятой.");
        }
    }

    public boolean requestConfirmation(@NotNull String prompt) {
        return number(prompt + " (1 - да, 0 - нет)", 0, 1) == 1;
    }

    public <T> T select(@NotNull String title, @NotNull List<T> values, @NotNull Function<T, String> label) {
        print("\n" + title);
        if (values.isEmpty()) {
            print("Список пуст.");
            return null;
        }
        for (int i = 0; i < values.size(); i++) {
            print((i + 1) + ". " + label.apply(values.get(i)));
        }
        print("0. Назад");
        int index = number("Выбор", 0, values.size());
        return index == 0 ? null : values.get(index - 1);
    }
}
