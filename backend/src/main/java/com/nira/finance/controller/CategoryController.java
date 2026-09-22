package com.nira.finance.controller;

import com.nira.finance.model.Category;
import com.nira.finance.repository.CategoryRepository;
import com.nira.finance.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<Category> list() {
        return categoryRepository.findByUserId(CurrentUser.id());
    }

    @PostMapping
    public Category create(@Valid @RequestBody Category category) {
        category.setUserId(CurrentUser.id());
        category.setId(null);
        return categoryRepository.save(category);
    }
}
