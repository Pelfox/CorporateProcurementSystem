package com.team.corporate.backend.controllers;


import com.team.corporate.backend.dto.CategoryResponse;
import com.team.corporate.backend.dto.CreateCategoryRequest;
import com.team.corporate.backend.dto.UpdateCategoryRequest;
import com.team.corporate.backend.search.CategoryFilter;
import com.team.corporate.backend.services.CategoriesSearchService;
import com.team.corporate.core.entities.Category;
import com.team.corporate.core.exceptions.CategoryNotFoundException;
import com.team.corporate.core.services.CategoriesService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoriesController {
    private final CategoriesService categories;
    private final CategoriesSearchService search;
    public CategoriesController(CategoriesService categories, CategoriesSearchService search) {
        this.categories = categories;
        this.search = search;
    }

    @GetMapping
    public PagedModel<CategoryResponse> getCategories(
            @Valid @RequestBody CategoryFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Sort sort
    ){
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategory(@PathVariable("id") UUID id){
        return categories.getCategory(id)
                .map(CategoryResponse::of)
                .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена."));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ){
        var category = categories.createCategory(request.name());

        return ResponseEntity
                .created(URI.create("api/categories/" + category.getId()))
                .body(CategoryResponse.of(category));

    }

    @PatchMapping("/{id}")
    public CategoryResponse updateCategory(
            @PathVariable("id") UUID id,
            @RequestBody UpdateCategoryRequest request
    ){
        var updatedCategory = categories.updateCategory(id, request.name());
        return CategoryResponse.of(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(){

    }
}
