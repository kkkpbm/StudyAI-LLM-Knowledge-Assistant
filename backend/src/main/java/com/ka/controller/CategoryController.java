package com.ka.controller;

import com.ka.common.Result;
import com.ka.entity.Category;
import com.ka.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<List<Category>> tree(Authentication auth) {
        return Result.ok(categoryService.tree(getUserId(auth)));
    }

    @PostMapping
    public Result<Category> create(Authentication auth, @RequestBody Category category) {
        return Result.ok(categoryService.create(getUserId(auth), category));
    }

    @PutMapping("/{id}")
    public Result<Category> update(Authentication auth, @PathVariable Long id, @RequestBody Category category) {
        return Result.ok(categoryService.update(getUserId(auth), id, category));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(Authentication auth, @PathVariable Long id) {
        categoryService.delete(getUserId(auth), id);
        return Result.ok();
    }
}
