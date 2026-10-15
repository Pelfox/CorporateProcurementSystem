package com.team.corporate.controllers.utils;

import com.team.corporate.utils.EntityValidation;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.math.BigDecimal;

public final class MoneyInput {
    private MoneyInput() {
    }

    public static void configure(TextField field) {
        field.setTextFormatter(new TextFormatter<String>(change ->
                TextLengthInput.proposedText(change).matches("[0-9]{0,8}([.,][0-9]{0,2})?") ? change : null));
    }

    public static BigDecimal value(TextField field) {
        String text = field.getText();
        if (text == null || !text.matches("[0-9]{1,8}([.,][0-9]{1,2})?")) return null;
        try {
            return EntityValidation.requireMoney(new BigDecimal(text.replace(',', '.')), "Цена");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
