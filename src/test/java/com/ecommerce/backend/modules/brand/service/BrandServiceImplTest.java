package com.ecommerce.backend.modules.brand.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.common.service.FileStorageService;
import com.ecommerce.backend.modules.brand.dto.request.BrandRequest;
import com.ecommerce.backend.modules.brand.dto.response.BrandResponse;
import com.ecommerce.backend.modules.brand.entity.Brand;
import com.ecommerce.backend.modules.brand.enums.BrandStatus;
import com.ecommerce.backend.modules.brand.mapper.BrandMapper;
import com.ecommerce.backend.modules.brand.repository.BrandRepository;
import com.ecommerce.backend.modules.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrandServiceImpl Unit Test Suite")
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private BrandMapper brandMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private BrandServiceImpl brandService;

    private Brand testBrand;
    private BrandRequest testRequest;
    private BrandResponse testResponse;
    private MultipartFile mockLogoFile;

    @BeforeEach
    void setUp() {
        testBrand = new Brand();
        testBrand.setId(1L);
        testBrand.setName("Apple");
        testBrand.setStatus(BrandStatus.ACTIVE);
        testBrand.setLogo("https://cdn.example.com/brands/apple.png");
        testBrand.setProducts(new ArrayList<>());

        testRequest = new BrandRequest();
        testRequest.setName("Apple");
        testRequest.setStatus(BrandStatus.ACTIVE);

        testResponse = new BrandResponse();
        testResponse.setId(1L);
        testResponse.setName("Apple");
        testResponse.setStatus(BrandStatus.ACTIVE);
        testResponse.setLogo("https://cdn.example.com/brands/apple.png");

        mockLogoFile = mock(MultipartFile.class);
    }

    // =========================================================================
    // AC-BRAND-01: Lấy danh sách thương hiệu (getAllBrands)
    // =========================================================================
    @Nested
    @DisplayName("AC-BRAND-01: Get All Brands Tests")
    class GetAllBrandsTests {

        @Test
        @DisplayName("TC-BRAND-01 [Positive]: Should return list of all brands")
        void getAllBrands_Success() {
            when(brandRepository.findAll()).thenReturn(List.of(testBrand));
            when(brandMapper.toResponse(testBrand)).thenReturn(testResponse);

            List<BrandResponse> results = brandService.getAllBrands();

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("Apple", results.get(0).getName());
            verify(brandRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("TC-BRAND-02 [Boundary]: Should return empty list when no brands exist")
        void getAllBrands_Empty_ReturnsEmptyList() {
            when(brandRepository.findAll()).thenReturn(Collections.emptyList());

            List<BrandResponse> results = brandService.getAllBrands();

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    // =========================================================================
    // AC-BRAND-02: Tìm kiếm thương hiệu (searchBrands)
    // =========================================================================
    @Nested
    @DisplayName("AC-BRAND-02: Search Brands Tests")
    class SearchBrandsTests {

        @Test
        @DisplayName("TC-BRAND-03 [Positive]: Should return brands matching keyword")
        void searchBrands_Success() {
            when(brandRepository.findByNameContaining("App")).thenReturn(List.of(testBrand));
            when(brandMapper.toResponse(testBrand)).thenReturn(testResponse);

            List<BrandResponse> results = brandService.searchBrands("App");

            assertNotNull(results);
            assertEquals(1, results.size());
            verify(brandRepository, times(1)).findByNameContaining("App");
        }
    }

    // =========================================================================
    // AC-BRAND-03: Tạo mới thương hiệu (createBrand)
    // =========================================================================
    @Nested
    @DisplayName("AC-BRAND-03: Create Brand Tests")
    class CreateBrandTests {

        @Test
        @DisplayName("TC-BRAND-04 [Positive]: Should upload logo and create brand successfully")
        void createBrand_Success() {
            when(brandRepository.existsByName("Apple")).thenReturn(false);
            when(brandMapper.toEntity(testRequest)).thenReturn(testBrand);
            when(fileStorageService.uploadFile(mockLogoFile, "brands")).thenReturn("https://cdn/logo.png");
            when(brandRepository.save(testBrand)).thenReturn(testBrand);
            when(brandMapper.toResponse(testBrand)).thenReturn(testResponse);

            BrandResponse response = brandService.createBrand(testRequest, mockLogoFile);

            assertNotNull(response);
            assertEquals("Apple", response.getName());
            verify(fileStorageService, times(1)).uploadFile(mockLogoFile, "brands");
            verify(brandRepository, times(1)).save(testBrand);
        }

        @Test
        @DisplayName("TC-BRAND-05 [Negative]: Throw DuplicateResourceException when brand name already exists")
        void createBrand_DuplicateName_ThrowsException() {
            when(brandRepository.existsByName("Apple")).thenReturn(true);

            DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () ->
                    brandService.createBrand(testRequest, mockLogoFile)
            );
            assertThat(ex.getMessage()).contains("already exists");
            verify(brandRepository, never()).save(any());
        }
    }

    // =========================================================================
    // AC-BRAND-04: Cập nhật thương hiệu (updateBrand)
    // =========================================================================
    @Nested
    @DisplayName("AC-BRAND-04: Update Brand Tests")
    class UpdateBrandTests {

        @Test
        @DisplayName("TC-BRAND-06 [Positive]: Should update brand without new logo")
        void updateBrand_WithoutNewLogo_Success() {
            testRequest.setName("Apple Inc");
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
            when(brandRepository.existsByNameAndIdNot("Apple Inc", 1L)).thenReturn(false);
            when(brandRepository.save(testBrand)).thenReturn(testBrand);
            when(brandMapper.toResponse(testBrand)).thenReturn(testResponse);

            BrandResponse response = brandService.updateBrand(1L, testRequest, null);

            assertNotNull(response);
            assertEquals("Apple Inc", testBrand.getName());
            verify(fileStorageService, never()).uploadFile(any(), any());
            verify(brandRepository, times(1)).save(testBrand);
        }

        @Test
        @DisplayName("TC-BRAND-07 [Positive]: Should update brand with new logo and delete old logo")
        void updateBrand_WithNewLogo_Success() {
            when(mockLogoFile.isEmpty()).thenReturn(false);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
            when(brandRepository.existsByNameAndIdNot("Apple", 1L)).thenReturn(false);
            when(fileStorageService.uploadFile(mockLogoFile, "brands")).thenReturn("https://cdn/new-logo.png");
            when(brandRepository.save(testBrand)).thenReturn(testBrand);
            when(brandMapper.toResponse(testBrand)).thenReturn(testResponse);

            BrandResponse response = brandService.updateBrand(1L, testRequest, mockLogoFile);

            assertNotNull(response);
            verify(fileStorageService, times(1)).deleteFile("https://cdn.example.com/brands/apple.png");
            verify(brandRepository, times(1)).save(testBrand);
        }

        @Test
        @DisplayName("TC-BRAND-08 [Negative]: Throw ResourceNotFoundException when brand ID not found")
        void updateBrand_NotFound_ThrowsException() {
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    brandService.updateBrand(999L, testRequest, null)
            );
            verify(brandRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-BRAND-09 [Negative]: Throw DuplicateResourceException when new name already in use")
        void updateBrand_DuplicateName_ThrowsException() {
            testRequest.setName("Samsung");
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
            when(brandRepository.existsByNameAndIdNot("Samsung", 1L)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () ->
                    brandService.updateBrand(1L, testRequest, null)
            );
            verify(brandRepository, never()).save(any());
        }
    }

    // =========================================================================
    // AC-BRAND-05: Xóa thương hiệu (deleteBrand)
    // =========================================================================
    @Nested
    @DisplayName("AC-BRAND-05: Delete Brand Tests")
    class DeleteBrandTests {

        @Test
        @DisplayName("TC-BRAND-10 [Positive]: Delete brand without products and cleanup logo file")
        void deleteBrand_Success() {
            testBrand.setProducts(Collections.emptyList());
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

            brandService.deleteBrand(1L);

            verify(fileStorageService, times(1)).deleteFile("https://cdn.example.com/brands/apple.png");
            verify(brandRepository, times(1)).delete(testBrand);
        }

        @Test
        @DisplayName("TC-BRAND-11 [Negative]: Throw BadRequestException when brand still has associated products")
        void deleteBrand_HasProducts_ThrowsBadRequest() {
            testBrand.setProducts(List.of(new Product()));
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    brandService.deleteBrand(1L)
            );
            assertThat(ex.getMessage()).contains("Cannot delete this brand because it contains products");
            verify(brandRepository, never()).delete(any());
        }

        @Test
        @DisplayName("TC-BRAND-12 [Negative]: Throw ResourceNotFoundException when brand not found")
        void deleteBrand_NotFound_ThrowsException() {
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    brandService.deleteBrand(999L)
            );
            verify(brandRepository, never()).delete(any());
        }
    }
}
