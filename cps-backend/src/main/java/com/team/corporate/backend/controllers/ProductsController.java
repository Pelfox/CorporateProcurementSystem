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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Товары", description = "Каталог товаров и привязка тегов. Чтение: USER, MANAGER и ADMIN; изменение: MANAGER и ADMIN после смены временного пароля.")
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
    @Operation(summary = "Получить страницу товаров", description = "Необязательные query-фильтры name, categoryId, minPrice и maxPrice объединяются условием И. "
            + "name ищет подстроку без учёта регистра; границы цены включительны, неотрицательны, minPrice ≤ maxPrice. "
            + "page с 0, size от 1 до 100; по умолчанию 0 и 20. sort: sku, name, price в формате sort=price,desc. "
            + "Ответ содержит content и page с size, number, totalElements и totalPages.")
    public PagedModel<ProductResponse> getProducts(
            @ParameterObject @Valid @ModelAttribute ProductFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @ParameterObject Sort sort
    ) {
        return new PagedModel<>(search.search(filter, page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить товар по UUID", description = "Отсутствующий или удалённый товар — 404.")
    public ProductResponse getProduct(
            @PathVariable("id") UUID id
    ) {
        return products.getProduct(id)
                .map(ProductResponse::of)
                .orElseThrow(() -> new ProductNotFoundException("Товар не найден"));
    }

    @GetMapping("/{id}/tags")
    @Operation(summary = "Получить теги товара", description = "Возвращает список без пагинации, отсортированный по имени и UUID тега. Отсутствующий или удалённый товар — 404.")
    public List<TagResponse> getProductTags(@PathVariable("id") UUID id) {
        return tags.getProductTags(id);
    }

    @PutMapping("/{id}/tags/{tagId}")
    @Operation(summary = "Привязать тег к товару", description = "Только MANAGER и ADMIN. Товар и тег должны существовать; иначе 404. Повторная привязка не создаёт дубликат.")
    @ApiResponse(responseCode = "204", description = "Тег привязан к товару")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addProductTag(@PathVariable("id") UUID id, @PathVariable("tagId") UUID tagId) {
        tags.addToProduct(id, tagId);
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    @Operation(summary = "Отвязать тег от товара", description = "Только MANAGER и ADMIN. Товар и тег должны существовать; иначе 404. Сам тег остаётся в справочнике.")
    @ApiResponse(responseCode = "204", description = "Тег отвязан от товара")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProductTag(@PathVariable("id") UUID id, @PathVariable("tagId") UUID tagId) {
        tags.removeFromProduct(id, tagId);
    }

    @PostMapping
    @Operation(summary = "Создать товар", description = "Только MANAGER и ADMIN. Обязательны name, уникальный sku, categoryId и положительная price. "
            + "Ответ содержит созданный товар и заголовок Location.")
    @ApiResponse(responseCode = "201", description = "Товар создан")
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
    @Operation(summary = "Удалить товар", description = "Только MANAGER и ADMIN. Нельзя удалить товар, используемый в позициях заказов. "
            + "Выполняет мягкое удаление товара и его складских остатков; восстановление и окончательное удаление доступны ADMIN через /api/admin/deleted.")
    @ApiResponse(responseCode = "204", description = "Товар удалён")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @PathVariable("id") UUID id
    ){
        products.deleteProduct(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменить товар", description = "Только MANAGER и ADMIN. Обновляет переданные name, sku, categoryId и price; null сохраняет прежнее значение. "
            + "Цена положительна. Отсутствующий или удалённый товар — 404.")
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

