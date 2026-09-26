package com.team.corporate.services;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public interface CsvExportService {
    @NotNull
    Path exportAll(@NotNull UUID managerId, @NotNull Path directory) throws IOException;
}
