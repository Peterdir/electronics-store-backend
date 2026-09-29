package com.ecommerce.backend.modules.category.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.category.dto.request.CategoryRequest;
import com.ecommerce.backend.modules.category.dto.response.CategoryResponse;
import com.ecommerce.backend.modules.category.entity.Category;
import com.ecommerce.backend.modules.category.enums.CategoryStatus;
import com.ecommerce.backend.modules.category.mapper.CategoryMapper;
import com.ecommerce.backend.modules.category.repository.CategoryRepository;
import com.ecommerce.backend.modules.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Unit Test Suite")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private CategoryRequest testRequest;
    private CategoryResponse testResponse;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Laptops");
        testCategory.setStatus(CategoryStatus.ACTIVE);
        testCategory.setProducts(new ArrayList<>());

        testRequest = new CategoryRequest();
        testRequest.setName("Laptops");
        testRequest.setStatus(CategoryStatus.ACTIVE);

        testResponse = new CategoryResponse();
        testResponse.setId(1L);
        testResponse.setName("Laptops");
        testResponse.setStatus(CategoryStatus.ACTIVE);
    }

    // =========================================================================
    // AC-CAT-01: Lấy danh sách danh mục (getAllCategories)
    // =========================================================================
    @Nested
    @DisplayName("AC-CAT-01: Get All Categories Tests")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("TC-CAT-01 [Positive]: Should return list of all categories")
        void getAllCategories_Success() {
            when(categoryRepository.findAll()).thenReturn(List.of(testCategory));
            when(categoryMapper.toResponse(testCategory)).thenReturn(testResponse);

            List<CategoryResponse> results = categoryService.getAllCategories();

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("Laptops", results.get(0).getName());
            verify(categoryRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("TC-CAT-02 [Boundary]: Should return empty list when no categories exist")
        void getAllCategories_Empty_ReturnsEmptyList() {
            when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

            List<CategoryResponse> results = categoryService.getAllCategories();

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    // =========================================================================
    // AC-CAT-02: Tìm kiếm danh mục (searchCategories)
    // =========================================================================
    @Nested
    @DisplayName("AC-CAT-02: Search Categories Tests")
    class SearchCategoriesTests {

        @Test
        @DisplayName("TC-CAT-03 [Positive]: Should return categories matching keyword")
        void searchCategories_Success() {
            when(categoryRepository.findByNameContaining("Lap")).thenReturn(List.of(testCategory));
            when(categoryMapper.toResponse(testCategory)).thenReturn(testResponse);

            List<CategoryResponse> results = categoryService.searchCategories("Lap");

            assertNotNull(results);
            assertEquals(1, results.size());
            verify(categoryRepository, times(1)).findByNameContaining("Lap");
        }
    }

    // =========================================================================
    // AC-CAT-03: Tạo mới danh mục (createCategory)
    // =========================================================================
    @Nested
    @DisplayName("AC-CAT-03: Create Category Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("TC-CAT-04 [Positive]: Should create category successfully")
        void createCategory_Success() {
            when(categoryRepository.existsByName("Laptops")).thenReturn(false);
            when(categoryMapper.toEntity(testRequest)).thenReturn(testCategory);
            when(categoryRepository.save(testCategory)).thenReturn(testCategory);
            when(categoryMapper.toResponse(testCategory)).thenReturn(testResponse);

            CategoryResponse response = categoryService.createCategory(testRequest);

            assertNotNull(response);
            assertEquals("Laptops", response.getName());
            verify(categoryRepository, times(1)).save(testCategory);
        }

        @Test
        @DisplayName("TC-CAT-05 [Negative]: Throw DuplicateResourceException when name exists")
        void createCategory_DuplicateName_ThrowsException() {
            when(categoryRepository.existsByName("Laptops")).thenReturn(true);

            DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                    categoryService.createCategory(testRequest)
            );
            assertThat(ex.getMessage()).contains("already exists");
            verify(categoryRepository, never()).save(any());
        }
    }

    // =========================================================================
    // AC-CAT-04: Cập nhật danh mục (updateCategory)
    // =========================================================================
    @Nested
    @DisplayName("AC-CAT-04: Update Category Tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("TC-CAT-06 [Positive]: Should update category successfully")
        void updateCategory_Success() {
            testRequest.setName("Gaming Laptops");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsByNameAndIdNot("Gaming Laptops", 1L)).thenReturn(false);
            when(categoryRepository.save(testCategory)).thenReturn(testCategory);
            when(categoryMapper.toResponse(testCategory)).thenReturn(testResponse);

            CategoryResponse response = categoryService.updateCategory(1L, testRequest);

            assertNotNull(response);
            assertEquals("Gaming Laptops", testCategory.getName());
            verify(categoryRepository, times(1)).save(testCategory);
        }

        @Test
        @DisplayName("TC-CAT-07 [Negative]: Throw ResourceNotFoundException when category not found")
        void updateCategory_NotFound_ThrowsException() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    categoryService.updateCategory(999L, testRequest)
            );
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-CAT-08 [Negative]: Throw DuplicateResourceException when new name already in use by another category")
        void updateCategory_DuplicateName_ThrowsException() {
            testRequest.setName("Existing Category");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsByNameAndIdNot("Existing Category", 1L)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () ->
                    categoryService.updateCategory(1L, testRequest)
            );
            verify(categoryRepository, never()).save(any());
        }
    }

    // =========================================================================
    // AC-CAT-05: Xóa danh mục (deleteCategory)
    // =========================================================================
    @Nested
    @DisplayName("AC-CAT-05: Delete Category Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("TC-CAT-09 [Positive]: Delete empty category successfully")
        void deleteCategory_Success() {
            testCategory.setProducts(Collections.emptyList());
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            categoryService.deleteCategory(1L);

            verify(categoryRepository, times(1)).delete(testCategory);
        }

        @Test
        @DisplayName("TC-CAT-10 [Negative]: Throw BadRequestException when category has associated products")
        void deleteCategory_HasProducts_ThrowsBadRequest() {
            testCategory.setProducts(List.of(new Product()));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    categoryService.deleteCategory(1L)
            );
            assertThat(ex.getMessage()).contains("Cannot delete this category because it contains products");
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("TC-CAT-11 [Negative]: Throw ResourceNotFoundException when category not found")
        void deleteCategory_NotFound_ThrowsException() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    categoryService.deleteCategory(999L)
            );
            verify(categoryRepository, never()).delete(any());
        }
    }
}
