package com.ecommerce.backend.modules.category.service;

import com.ecommerce.backend.modules.category.dto.request.CategoryRequest;
import com.ecommerce.backend.modules.category.dto.response.CategoryResponse;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> searchCategories(String keyword);

    CategoryResponse createCategory(CategoryRequest request, MultipartFile imageFile);

    CategoryResponse updateCategory(Long id, CategoryRequest request, MultipartFile imageFile);

    void deleteCategory(Long id);
}
