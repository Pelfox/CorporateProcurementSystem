package com.team.corporate.desktop.controllers.utils;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;

public final class IntegerInput {
    private IntegerInput() {
    }

    public static void configure(TextField field, int initialValue) {
        configure(field, initialValue, Integer.MAX_VALUE);
    }

    public static void configure(TextField field, int initialValue, int maximum) {
        field.setTooltip(new Tooltip("Целое число от " + initialValue + " до " + maximum + "."));
        field.setTextFormatter(new TextFormatter<String>(change -> {
            String text = TextLengthInput.proposedText(change);
            if (text.isEmpty()) return change;
            if (!text.matches("[0-9]{1,10}")) return null;
            try {
                return Integer.parseInt(text) <= maximum ? change : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }));
        field.setText(String.valueOf(initialValue));
    }

    public static Integer value(TextField field) {
        if (field.getText() == null || !field.getText().matches("[0-9]{1,10}")) return null;
        try {
            return Integer.valueOf(field.getText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
