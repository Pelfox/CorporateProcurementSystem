package com.team.corporate.config;

import org.jetbrains.annotations.NotNull;

import java.util.Properties;
import java.time.DateTimeException;
import java.time.ZoneId;

public class ApplicationConfiguration {
    private String databaseUrl;
    private String databaseUsername;
    private String databasePassword;

    private String hibernateDdl;
    private String jdbcTimeZone;

    @NotNull
    public static ApplicationConfiguration fromProperties(@NotNull Properties properties) {
        ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.databaseUrl = required(properties, "db.url");
        configuration.databaseUsername = required(properties, "db.username");
        configuration.databasePassword = properties.getProperty("db.password");
        configuration.hibernateDdl = required(properties, "hibernate.ddl.auto");
        configuration.jdbcTimeZone = required(properties, "hibernate.jdbc.time_zone");
        return configuration;
    }

    @NotNull
    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Не заполнен параметр настроек " + key + ".");
        }
        return value.strip();
    }

    @NotNull
    public String getDatabaseUrl() {
        return databaseUrl;
    }

    @NotNull
    public String getDatabaseUsername() {
        return databaseUsername;
    }

    @NotNull
    public String getDatabasePassword() {
        return databasePassword;
    }

    @NotNull
    public String getHibernateDdl() {
        return hibernateDdl;
    }

    @NotNull
    public String getJdbcTimeZone() {
        return jdbcTimeZone;
    }
}
