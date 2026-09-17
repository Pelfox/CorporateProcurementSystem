package com.team.corporate.config;

import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class ApplicationConfiguration {
    private String databaseUrl;
    private String databaseUsername;
    private String databasePassword;

    private String hibernateDdl;
    private String jdbcTimeZone;

    @NotNull
    public static ApplicationConfiguration fromProperties(@NotNull Properties properties) {
        ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.databaseUrl = properties.getProperty("db.url");
        configuration.databaseUsername = properties.getProperty("db.username");
        configuration.databasePassword = properties.getProperty("db.password");
        configuration.hibernateDdl = properties.getProperty("hibernate.ddl.auto");
        configuration.jdbcTimeZone = properties.getProperty("hibernate.jdbc.time_zone");
        return configuration;
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
