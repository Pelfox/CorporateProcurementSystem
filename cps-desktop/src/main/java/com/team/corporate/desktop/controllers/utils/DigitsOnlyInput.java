package com.team.corporate.desktop.controllers.utils;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;

import java.math.BigDecimal;

public final class DigitsOnlyInput {
    public static final int MAX_DIGITS = 18;

    private DigitsOnlyInput() {
    }

    public static BigDecimal value(TextField field) {
        String text = field.getText();
        if (text == null || !text.matches("[0-9]{1," + MAX_DIGITS + "}")) {
            throw new NumberFormatException("Введите не более " + MAX_DIGITS + " цифр.");
        }
        return new BigDecimal(text);
    }

    public static void configure(TextField... fields) {
        for (TextField field : fields) {
            field.setTextFormatter(new TextFormatter<String>(change ->
                    TextLengthInput.proposedText(change).matches("[0-9]{0," + MAX_DIGITS + "}") ? change : null));
            field.setTooltip(new Tooltip("Только цифры, не более " + MAX_DIGITS + " знаков."));
        }
    }
}
