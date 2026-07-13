package com.vyaparsetu.catalog.controller;

import com.vyaparsetu.catalog.entity.Category;
import com.vyaparsetu.catalog.repository.CategoryRepository;
import com.vyaparsetu.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Product categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ApiResponse<List<Category>> all() {
        return ApiResponse.ok(categoryRepository.findAll());
    }
}
