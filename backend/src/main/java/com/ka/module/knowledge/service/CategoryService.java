package com.ka.module.knowledge.service;

import com.ka.module.knowledge.domain.Category;

import java.util.List;

public interface CategoryService {
    List<Category> tree(Long userId);

    Category create(Long userId, Category category);

    Category update(Long userId, Long id, Category category);

    void delete(Long userId, Long id);
}

