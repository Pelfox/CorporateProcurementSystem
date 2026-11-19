package com.team.corporate.core.services;

import com.team.corporate.core.dto.CsvApplyResponse;
import com.team.corporate.core.dto.CsvValidationResponse;
import com.team.corporate.core.dto.ImportMode;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface CsvImportService {
    @NotNull
    CsvValidationResponse validateProducts(UUID managerId, byte[] csv, ImportMode mode);

    @NotNull
    CsvApplyResponse applyProducts(UUID managerId, UUID importId);
}
