package com.team.corporate.domain.repository;


import com.team.corporate.domain.models.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product add(Product product);
    List<Product> getAll();
    Optional<Product> getById(UUID id);
    Optional<Product> getBySKU(String sku);
    Product update(Product product);
    void delete(UUID id);
}
