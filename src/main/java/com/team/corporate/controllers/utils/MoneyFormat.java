package com.team.corporate.controllers.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public final class MoneyFormat {
    private static final DecimalFormat FORMAT = build();
    private MoneyFormat() {}

    public static String format(BigDecimal amount) { return FORMAT.format(amount); }

    private static DecimalFormat build() {
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator(' ');
        s.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", s);
    }
}
