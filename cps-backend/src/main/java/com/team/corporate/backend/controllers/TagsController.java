package com.team.corporate.backend.controllers;

import com.team.corporate.backend.dto.CreateTagRequest;
import com.team.corporate.backend.dto.TagResponse;
import com.team.corporate.backend.dto.UpdateTagRequest;
import com.team.corporate.backend.services.TagsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Теги", description = "Справочник тегов товаров. Чтение: USER, MANAGER и ADMIN; изменение: MANAGER и ADMIN после смены временного пароля.")
public class TagsController {
    private final TagsService tags;

    public TagsController(TagsService tags) {
        this.tags = tags;
    }

    @GetMapping
    @Operation(summary = "Получить все теги", description = "Возвращает список без пагинации, отсортированный по имени и UUID. Удалённые теги не включаются.")
    public List<TagResponse> findAll() {
        return tags.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить тег по UUID", description = "Отсутствующий или удалённый тег — 404.")
    public TagResponse getTag(@PathVariable("id") UUID id) {
        return tags.getById(id);
    }

    @PostMapping
    @Operation(summary = "Создать тег", description = "Только MANAGER и ADMIN. Передайте непустое name. Ответ содержит созданный тег и заголовок Location; привязка к товарам выполняется отдельно.")
    @ApiResponse(responseCode = "201", description = "Тег создан")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        var tag = tags.create(request.name());
        return ResponseEntity.created(URI.create("/api/tags/" + tag.id())).body(tag);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Переименовать тег", description = "Только MANAGER и ADMIN. Передайте непустое name. Отсутствующий или удалённый тег — 404.")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public TagResponse updateTag(@PathVariable("id") UUID id, @Valid @RequestBody UpdateTagRequest request) {
        return tags.update(id, request.name());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить тег", description = "Только MANAGER и ADMIN. Выполняет мягкое удаление; повторный вызов для отсутствующего или уже удалённого тега также возвращает 204.")
    @ApiResponse(responseCode = "204", description = "Тег удалён или уже отсутствует")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable("id") UUID id) {
        tags.delete(id);
    }
}
