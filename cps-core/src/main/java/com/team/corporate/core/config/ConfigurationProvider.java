package com.team.corporate.core.config;

import org.jetbrains.annotations.NotNull;

public interface ConfigurationProvider {
    @NotNull
    ApplicationConfiguration getConfiguration();
}
