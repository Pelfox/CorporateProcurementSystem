package com.team.corporate.config;

import org.jetbrains.annotations.NotNull;

public interface ConfigurationProvider {
    @NotNull ApplicationConfiguration getConfiguration();
}
