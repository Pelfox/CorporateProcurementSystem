package com.team.corporate.backend.controllers;


import com.team.corporate.backend.dto.CreateProductRequest;
import com.team.corporate.backend.dto.ProductResponse;
import com.team.corporate.backend.dto.UpdateProductRequest;
import com.team.corporate.backend.dto.TagResponse;
import com.team.corporate.backend.search.ProductFilter;
import com.team.corporate.backend.services.ProductsSearchService;
import com.team.corporate.backend.services.TagsService;
import com.team.corporate.core.exceptions.ProductNotFoundException;
import com.team.corporate.core.services.ProductsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductsController {

    private final ProductsService products;
    private final ProductsSearchService search;
    private final TagsService tags;


    public ProductsController(ProductsService products, ProductsSearchService search, TagsService tags)
    {
        this.products = products;
        this.search = search;
        this.tags = tags;
    }

    @GetMapping
    public PagedModel<ProductResponse> getProducts(
            @Valid @ModelAttribute ProductFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Sort sort
    ) {
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(
            @PathVariable("id") UUID id
    ) {
        return products.getProduct(id)
                .map(ProductResponse::of)
                .orElseThrow(() -> new ProductNotFoundException("Товар не найден"));
    }

    @GetMapping("/{id}/tags")
    public List<TagResponse> getProductTags(@PathVariable("id") UUID id) {
        return tags.getProductTags(id);
    }

    @PutMapping("/{id}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addProductTag(@PathVariable("id") UUID id, @PathVariable("tagId") UUID tagId) {
        tags.addToProduct(id, tagId);
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProductTag(@PathVariable("id") UUID id, @PathVariable("tagId") UUID tagId) {
        tags.removeFromProduct(id, tagId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        var product = products.createProduct(
              request.name(),
              request.sku(),
              request.categoryId(),
              request.price()
        );

        return ResponseEntity
              .created(URI.create("/api/products/" + product.getId()))
              .body(ProductResponse.of(product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @PathVariable("id") UUID id
    ){
        products.deleteProduct(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ProductResponse updateProduct(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        var updatedProduct = products.updateProduct(
                id,
                request.name(),
                request.sku(),
                request.categoryId(),
                request.price()
        );

        return ProductResponse.of(updatedProduct);
    }
}

