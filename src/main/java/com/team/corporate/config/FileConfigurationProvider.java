package com.team.corporate.config;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FileConfigurationProvider implements ConfigurationProvider {
    private static final String CONFIGURATION_FILE_NAME = "application.properties";

    @Override
    @NotNull
    public ApplicationConfiguration getConfiguration() {
        try (InputStream input = FileConfigurationProvider.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME)) {
            if (input == null) {
                throw new RuntimeException("Configuration file was not found");
            }

            Properties properties = new Properties();
            properties.load(input);

            return ApplicationConfiguration.fromProperties(properties);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
