package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.CategoryDto;
import com.team.corporate.backend.dto.SearchQuery;
import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.core.services.CategoriesService;
import com.team.corporate.core.services.ProductsService;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.team.corporate.backend.http.QueryExtractor.*;

@RestController
@RequestMapping("/api/categories")
public class CategoriesHandler {
    private final CategoriesService categories;
    private final ProductsService products;
    private final ApiExecutor api;

    public CategoriesHandler(
            @NotNull CategoriesService categories,
            @NotNull ProductsService products,
            @NotNull ApiExecutor api
    ) {
        this.categories = categories;
        this.products = products;
        this.api = api;
    }

    @GetMapping
    public List<CategoryDto> list(@Valid @ModelAttribute SearchQuery query) {
        return api.execute(() -> {
            var counts = new HashMap<UUID, Long>();
            for (var product : products.getAll()) {
                if (product.getCategory() != null) counts.merge(product.getCategory().getId(), 1L, Long::sum);
            }
            return categories.getAll().stream().filter(category -> contains(category.getName(), normalize(query.q())))
                    .map(category -> CategoryDto.from(category, counts.getOrDefault(category.getId(), 0L)))
                    .sorted(Comparator.comparing(CategoryDto::name, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(CategoryDto::id)).toList();
        });
    }

    @GetMapping("/{id}")
    public CategoryDto get(@PathVariable UUID id) {
        return find(id);
    }

    @PostMapping
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryDto.Request body) {
        var result = api.execute(() -> CategoryDto.from(categories.createCategory(body.name()), 0));
        return ResponseEntity.created(URI.create("/api/categories/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable UUID id, @Valid @RequestBody CategoryDto.Request body) {
        api.run(() -> categories.updateCategory(id, body.name()));
        return find(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        api.run(() -> {
            categories.getCategory(id).orElseThrow(() -> ApiException.notFound("Категория"));
            categories.deleteCategory(id);
        });
        return ResponseEntity.noContent().build();
    }

    private CategoryDto find(UUID id) {
        return api.execute(() -> CategoryDto.from(categories.getCategory(id)
                .orElseThrow(() -> ApiException.notFound("Категория")), products.getAllByCategory(id).size()));
    }
}
