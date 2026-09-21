package com.ecommerce.backend.modules.category.service;

import com.ecommerce.backend.modules.category.dto.request.CategoryRequest;
import com.ecommerce.backend.modules.category.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> searchCategories(String keyword);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
