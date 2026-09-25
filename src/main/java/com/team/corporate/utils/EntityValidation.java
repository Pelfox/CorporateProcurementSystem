package com.team.corporate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class EntityValidation {
    public static final int TEXT_MAX_LENGTH = 255;
    public static final BigDecimal MAX_MONEY = new BigDecimal("99999999.99");

    public static <T> T requireNonNull(T value, @NotNull String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + ": значение обязательно.");
        }
        return value;
    }

    @NotNull
    public static String requireText(String value, @NotNull String name) {
        String result = optionalText(requireNonNull(value, name), name);
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException(name + ": значение не должно быть пустым.");
        }
        return result;
    }

    @Nullable
    public static String optionalText(@Nullable String value, @NotNull String name) {
        if (value == null) {
            return null;
        }
        if (value.length() > TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException(name + ": максимум " + TEXT_MAX_LENGTH + " символов.");
        }
        return value.strip();
    }

    public static int requireAtLeast(int value, int minimum, @NotNull String name) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + ": минимум " + minimum + ".");
        }
        return value;
    }

    @NotNull
    public static BigDecimal requireMoney(@NotNull BigDecimal value, @NotNull String name) {
        requireNonNull(value, name);
        if (value.signum() < 0 || value.compareTo(MAX_MONEY) > 0) {
            throw new IllegalArgumentException(name + ": введите сумму от 0 до 99999999,99.");
        }
        try {
            BigDecimal normalized = value.stripTrailingZeros();
            if (normalized.scale() > 2) {
                throw new ArithmeticException("Слишком много знаков после запятой");
            }
            return normalized.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(name + ": не более двух знаков после запятой.", e);
        }
    }
}
