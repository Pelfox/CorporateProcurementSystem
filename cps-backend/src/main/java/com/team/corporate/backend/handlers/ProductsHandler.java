package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.ProductDto;
import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.core.services.ProductsService;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.team.corporate.backend.http.QueryExtractor.*;

@RestController
@RequestMapping("/api/products")
public class ProductsHandler {
    private final ProductsService products;
    private final ApiExecutor api;

    public ProductsHandler(
            @NotNull ProductsService products,
            @NotNull ApiExecutor api
    ) {
        this.products = products;
        this.api = api;
    }

    @GetMapping
    public List<ProductDto> list(@Valid @ModelAttribute ProductDto.Query query) {
        String q = normalize(query.q());
        Comparator<ProductDto> comparator = switch (query.sort()) {
            case NAME_ASC -> Comparator.comparing(ProductDto::name, String.CASE_INSENSITIVE_ORDER);
            case NAME_DESC -> Comparator.comparing(ProductDto::name, String.CASE_INSENSITIVE_ORDER).reversed();
            case PRICE_ASC -> Comparator.comparing(ProductDto::price);
            case PRICE_DESC -> Comparator.comparing(ProductDto::price).reversed();
        };
        return api.execute(() -> {
            var source = query.categoryId() == null ? products.getAll() : products.getAllByCategory(query.categoryId());
            return source.stream().map(ProductDto::from)
                    .filter(product -> contains(product.name(), q) || contains(product.sku(), q)
                            || contains(product.id().toString(), q))
                    .filter(product -> query.minPrice() == null || product.price().compareTo(query.minPrice()) >= 0)
                    .filter(product -> query.maxPrice() == null || product.price().compareTo(query.maxPrice()) <= 0)
                    .sorted(comparator.thenComparing(ProductDto::id)).toList();
        });
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable UUID id) {
        return api.execute(() -> ProductDto.from(products.getProduct(id)
                .orElseThrow(() -> ApiException.notFound("Товар"))));
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@Valid @RequestBody ProductDto.Request body) {
        var result = api.execute(() -> ProductDto.from(products.createProduct(
                body.name(), body.sku(), body.categoryId(), body.price())));
        return ResponseEntity.created(URI.create("/api/products/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable UUID id, @Valid @RequestBody ProductDto.Request body) {
        return api.execute(() -> ProductDto.from(products.updateProduct(
                id, body.name(), body.sku(), body.categoryId(), body.price())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        api.run(() -> {
            products.getProduct(id).orElseThrow(() -> ApiException.notFound("Товар"));
            products.deleteProduct(id);
        });
        return ResponseEntity.noContent().build();
    }
}
