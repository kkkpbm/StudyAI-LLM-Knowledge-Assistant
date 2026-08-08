package com.ka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.common.BusinessException;
import com.ka.entity.Category;
import com.ka.mapper.CategoryMapper;
import com.ka.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> tree(Long userId) {
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId).orderByAsc(Category::getCreatedAt));
        Map<Long, List<Category>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(Category::getParentId));
        List<Category> roots = new ArrayList<>();
        for (Category c : all) {
            if (c.getParentId() == null || c.getParentId() == 0) {
                roots.add(c);
            }
        }
        return roots;
    }

    public Category create(Long userId, Category category) {
        category.setUserId(userId);
        categoryMapper.insert(category);
        return category;
    }

    public Category update(Long userId, Long id, Category category) {
        Category exist = categoryMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(404, "Category not found");
        }
        category.setId(id);
        categoryMapper.updateById(category);
        return category;
    }

    public void delete(Long userId, Long id) {
        Category exist = categoryMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(404, "Category not found");
        }
        categoryMapper.deleteById(id);
    }
}
