package com.team.corporate.utils;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class EntityValidation {
    public static int requireAtLeast(int value, int minimum, @NotNull String name) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + " must be at least " + minimum);
        }
        return value;
    }

    @NotNull
    public static BigDecimal requireMoney(@NotNull BigDecimal value, @NotNull String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must be at least 0");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(name + " must be representable with two decimal places", e);
        }
    }
}
