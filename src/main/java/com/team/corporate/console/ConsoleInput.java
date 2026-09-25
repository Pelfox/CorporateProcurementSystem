package com.team.corporate.console;

import com.team.corporate.utils.EntityValidation;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.LineReader;

import java.math.BigDecimal;
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
        return readText(prompt, false);
    }

    public String notes(@NotNull String prompt) {
        return readText(prompt, false);
    }

    public String required(@NotNull String prompt) {
        return readText(prompt, true);
    }

    private String readText(String prompt, boolean required) {
        while (true) {
            String value = reader.readLine(prompt + " (макс. " + EntityValidation.TEXT_MAX_LENGTH + " символов): ");
            try {
                return required ? EntityValidation.requireText(value, prompt)
                        : EntityValidation.optionalText(value, prompt);
            } catch (IllegalArgumentException e) {
                print(e.getMessage());
            }
        }
    }

    public int number(@NotNull String prompt, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Некорректный диапазон чисел.");
        }
        while (true) {
            try {
                int value = Integer.parseInt(numericText(prompt));
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
                String input = numericText(prompt).replace(',', '.');
                if (input.matches("[+]?[0-9]+(?:\\.[0-9]+)?")) {
                    return EntityValidation.requireMoney(new BigDecimal(input), "Цена");
                }
            } catch (IllegalArgumentException | ArithmeticException ignored) {
            }
            print("Введите цену от 0 до 99999999,99, не более двух знаков после запятой.");
        }
    }

    private String numericText(String prompt) {
        String value = reader.readLine(prompt + ": ");
        if (value.length() > EntityValidation.TEXT_MAX_LENGTH) {
            throw new NumberFormatException("Слишком длинное число.");
        }
        return value.strip();
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
