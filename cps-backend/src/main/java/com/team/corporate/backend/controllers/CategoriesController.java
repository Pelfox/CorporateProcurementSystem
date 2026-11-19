package com.team.corporate.backend.controllers;


import com.team.corporate.backend.dto.CategoryResponse;
import com.team.corporate.backend.dto.CreateCategoryRequest;
import com.team.corporate.backend.dto.UpdateCategoryRequest;
import com.team.corporate.backend.search.CategoryFilter;
import com.team.corporate.backend.services.CategoriesSearchService;
import com.team.corporate.core.entities.Category;
import com.team.corporate.core.exceptions.CategoryNotFoundException;
import com.team.corporate.core.services.CategoriesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Категории", description = "Справочник категорий товаров. Доступно USER, MANAGER и ADMIN после смены временного пароля.")
public class CategoriesController {
    private final CategoriesService categories;
    private final CategoriesSearchService search;
    public CategoriesController(CategoriesService categories, CategoriesSearchService search) {
        this.categories = categories;
        this.search = search;
    }

    @GetMapping
    @Operation(summary = "Получить страницу категорий", description = "Фильтр name передаётся в JSON-теле и ищет подстроку без учёта регистра. "
            + "page с 0, size от 1 до 100; по умолчанию 0 и 20. Сортировка: sort=name,asc или name,desc. "
            + "Ответ содержит content и page с size, number, totalElements и totalPages.")
    public PagedModel<CategoryResponse> getCategories(
            @Valid @RequestBody CategoryFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @ParameterObject Sort sort
    ){
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить категорию по UUID", description = "Отсутствующая или удалённая категория — 404.")
    public CategoryResponse getCategory(@PathVariable("id") UUID id){
        return categories.getCategory(id)
                .map(CategoryResponse::of)
                .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена."));
    }

    @PostMapping
    @Operation(summary = "Создать категорию", description = "Передайте непустое name длиной до 255 символов. Ответ содержит созданную категорию и заголовок Location.")
    @ApiResponse(responseCode = "201", description = "Категория создана")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ){
        var category = categories.createCategory(request.name());

        return ResponseEntity
                .created(URI.create("api/categories/" + category.getId()))
                .body(CategoryResponse.of(category));

    }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменить категорию", description = "Обязателен непустой name длиной до 255 символов. Отсутствующая или удалённая категория — 404.")
    public CategoryResponse updateCategory(
            @PathVariable("id") UUID id,
            @RequestBody UpdateCategoryRequest request
    ){
        var updatedCategory = categories.updateCategory(id, request.name());
        return CategoryResponse.of(updatedCategory);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить категорию (не реализовано)", description = "Текущая реализация возвращает 200 без изменения данных; категория не удаляется.")
    @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "UUID категории",
            schema = @Schema(type = "string", format = "uuid"))
    public void deleteCategory(){

    }
}
