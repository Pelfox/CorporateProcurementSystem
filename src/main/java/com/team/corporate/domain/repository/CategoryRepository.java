package com.team.corporate.domain.repository;

import com.team.corporate.domain.models.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Category add(Category category);
    List<Category> getAll();
    Optional<Category> getById(int id);
    Category update(Category category);
    void delete(int id);
}
