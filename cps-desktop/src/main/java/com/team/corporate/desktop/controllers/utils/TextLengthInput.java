package com.team.corporate.desktop.controllers.utils;

import com.team.corporate.core.utils.EntityValidation;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;

public final class TextLengthInput {
    private TextLengthInput() {
    }

    public static void configure(TextInputControl field) {
        configure(field, null, null, "Поле");
    }

    public static String text(TextInputControl field) {
        return java.util.Objects.toString(field.getText(), "");
    }

    public static String proposedText(TextFormatter.Change change) {
        // JavaFX marks text as null before clearing the old content in setText(null).
        // getControlNewText() cannot read that intermediate range safely.
        return change.getControl() instanceof TextInputControl input && input.getText() == null
                ? change.getText() : change.getControlNewText();
    }

    public static boolean isValid(TextInputControl field, boolean required) {
        try {
            if (required) EntityValidation.requireText(field.getText(), "Поле");
            else EntityValidation.optionalText(field.getText(), "Поле");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void configure(TextInputControl field, Label counter, Label error, String name) {
        int limit = EntityValidation.TEXT_MAX_LENGTH;
        if (counter != null) counter.textProperty().bind(Bindings.createStringBinding(
                () -> text(field).length() + " / " + limit, field.textProperty()));
        field.setTooltip(new Tooltip("Не более " + limit + " символов."));
        if (error != null) {
            error.setVisible(false);
            error.managedProperty().bind(error.visibleProperty());
        }
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) return change;
            String proposed = proposedText(change);
            boolean tooLong = proposed.length() > limit;
            boolean unsupported = EntityValidation.hasUnsupportedCharacters(proposed);
            if (error != null) {
                error.setText(tooLong ? name + ": максимум " + limit + " символов."
                        : name + ": недопустимые управляющие символы.");
                error.setVisible(tooLong || unsupported);
            }
            return tooLong || unsupported ? null : change;
        }));
    }
}
