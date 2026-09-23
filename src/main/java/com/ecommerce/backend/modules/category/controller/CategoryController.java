package com.ecommerce.backend.modules.category.controller;

import com.ecommerce.backend.modules.category.dto.response.CategoryResponse;
import com.ecommerce.backend.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> searchCategories(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(categoryService.searchCategories(keyword));
        }
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
}
