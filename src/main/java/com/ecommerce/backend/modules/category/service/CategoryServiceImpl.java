package com.ecommerce.backend.modules.category.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.common.service.FileStorageService;
import com.ecommerce.backend.modules.category.repository.CategoryRepository;
import com.ecommerce.backend.modules.category.dto.request.CategoryRequest;
import com.ecommerce.backend.modules.category.dto.response.CategoryResponse;
import com.ecommerce.backend.modules.category.entity.Category;
import com.ecommerce.backend.modules.category.mapper.CategoryMapper;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    private static final String CATEGORY_IMAGE_FOLDER = "categories";

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> searchCategories(String keyword) {
        return categoryRepository.findByNameContaining(keyword).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request, MultipartFile imageFile) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("This category name already exists: " + request.getName());
        }

        Category category = categoryMapper.toEntity(request);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.uploadFile(imageFile, CATEGORY_IMAGE_FOLDER);
            category.setImageUrl(imageUrl);
        }

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request, MultipartFile imageFile) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (categoryRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("This category name already exists: " + request.getName());
        }

        category.setName(request.getName());
        category.setStatus(request.getStatus());

        if (imageFile != null && !imageFile.isEmpty()) {
            String oldImageUrl = category.getImageUrl();

            String newImageUrl = fileStorageService.uploadFile(imageFile, CATEGORY_IMAGE_FOLDER);
            category.setImageUrl(newImageUrl);

            if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                fileStorageService.deleteFile(oldImageUrl);
            }
        }

        Category updatedCategory = categoryRepository.saveAndFlush(category);

        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (productRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete this category because it contains products");
        }

        if (category.getImageUrl() != null && !category.getImageUrl().isBlank()) {
            fileStorageService.deleteFile(category.getImageUrl());
        }

        categoryRepository.delete(category);
    }
}
