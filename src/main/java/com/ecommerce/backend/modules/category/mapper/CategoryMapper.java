package com.ecommerce.backend.modules.category.mapper;

import com.ecommerce.backend.modules.category.dto.request.CategoryRequest;
import com.ecommerce.backend.modules.category.dto.response.CategoryResponse;
import com.ecommerce.backend.modules.category.entity.Category;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    private final ProductRepository productRepository;

    // Chuyển Request DTO sang Entity
    public Category toEntity(CategoryRequest request) {
        if (request == null) return null;

        return Category.builder()
                .name(request.getName())
                .status(request.getStatus())
                .build();
    }

    // Chuyển từ Entity sang Response
    public CategoryResponse toResponse(Category category) {
        if (category == null) return null;

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .status(category.getStatus())
                .totalProducts(productRepository.countByCategoryId(category.getId()))
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
