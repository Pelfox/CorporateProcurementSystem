package com.team.corporate.desktop.controllers.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeFormat {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private DateTimeFormat() {
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZONE);
    }

    public static LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : instant.atZone(ZONE).toLocalDate();
    }

    public static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }
}
