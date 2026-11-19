package com.team.corporate.backend.controllers;

import com.team.corporate.core.dto.CsvApplyResponse;
import com.team.corporate.core.dto.CsvValidationResponse;
import com.team.corporate.core.dto.ImportMode;
import com.team.corporate.core.services.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/imports")
@Tag(name = "Импорт CSV", description = "Двухэтапный импорт товаров: проверка файла, затем применение по importId. Только MANAGER после смены временного пароля.")
@PreAuthorize("hasRole('MANAGER')")
public class CsvImportController {
    private final CsvImportService importService;

    public CsvImportController(CsvImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/products/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Проверить CSV с товарами", description = "Передайте multipart-поле file: непустой CSV в UTF-8, до 2 МБ и 10 000 строк данных, разделитель — запятая. "
            + "Обязательные столбцы: sku,name,category_id,price; id необязателен, значение category_id может быть пустым. "
            + "CREATE_ONLY создаёт только новые артикулы; UPSERT также обновляет существующие. Проверка не меняет товары. "
            + "Ответ содержит importId, счётчики строк, canApply и ошибки; загрузка хранится в памяти 15 минут и теряется при перезапуске.")
    public CsvValidationResponse validateProducts(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "CSV-файл товаров в UTF-8") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Режим обработки существующих артикулов")
            @RequestParam(name = "mode", defaultValue = "CREATE_ONLY") ImportMode mode
    ) throws IOException {
        return importService.validateProducts(UUID.fromString(jwt.getSubject()), file.getBytes(), mode);
    }

    @PostMapping("/products/{importId}/apply")
    @Operation(summary = "Применить проверенный импорт товаров", description = "Используйте importId из проверки с canApply=true под той же учётной записью. "
            + "Применяет весь импорт в одной транзакции и возвращает createdRows и updatedRows; повторный вызов до истечения срока возвращает прежний результат. "
            + "Чужая, отсутствующая или просроченная загрузка — 404; ошибки проверки или изменившиеся данные — 409.")
    public CsvApplyResponse applyProducts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("importId") UUID importId
    ) {
        return importService.applyProducts(UUID.fromString(jwt.getSubject()), importId);
    }
}
