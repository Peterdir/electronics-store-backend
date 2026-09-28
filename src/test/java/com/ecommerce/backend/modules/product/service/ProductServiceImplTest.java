package com.ecommerce.backend.modules.product.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.common.service.FileStorageService;
import com.ecommerce.backend.modules.brand.entity.Brand;
import com.ecommerce.backend.modules.brand.repository.BrandRepository;
import com.ecommerce.backend.modules.category.entity.Category;
import com.ecommerce.backend.modules.category.repository.CategoryRepository;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.product.dto.request.AdminProductRequest;
import com.ecommerce.backend.modules.product.dto.response.ProductDetailsResponse;
import com.ecommerce.backend.modules.product.dto.response.VariantResponse;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.mapper.ProductMapper;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private ProductMapper productMapper;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @Mock
    private BrandRepository brandRepository;
    
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private Category testCategory;
    private Brand testBrand;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");

        testBrand = new Brand();
        testBrand.setId(1L);
        testBrand.setName("Apple");

        testProduct = Product.builder()
                .id(100L)
                .name("iPhone 15")
                .basePrice(new BigDecimal("999"))
                .status(ProductStatus.ACTIVE)
                .category(testCategory)
                .brand(testBrand)
                .variants(new ArrayList<>())
                .build();
    }

    // ==========================================
    // CREATE PRODUCT TESTS
    // ==========================================

    @Test
    @DisplayName("Create Product: Success without variants and images")
    void createProduct_Success_NoVariantsNoImages() {
        AdminProductRequest request = new AdminProductRequest();
        request.setName("New Phone");
        request.setBasePrice(new BigDecimal("500"));
        request.setCategoryId(1L);
        request.setBrandId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

        productService.createProduct(request, null);

        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Create Product: Should throw ResourceNotFoundException when Category missing")
    void createProduct_Fail_CategoryNotFound() {
        AdminProductRequest request = new AdminProductRequest();
        request.setCategoryId(99L);

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> 
            productService.createProduct(request, null)
        );
        assertThat(ex.getMessage()).isEqualTo("Category not found");
        verify(productRepository, never()).save(any());
    }

    // ==========================================
    // CHANGE PRODUCT STATUS TESTS
    // ==========================================

    @Test
    @DisplayName("Change Status: Success")
    void changeProductStatus_Success() {
        ProductVariant variant = ProductVariant.builder().status(ProductStatus.ACTIVE).build();
        testProduct.getVariants().add(variant);
        testProduct.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        productService.changeProductStatus(100L, ProductStatus.INACTIVE);

        assertThat(testProduct.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(variant.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("Change Status: Fail if already in that status")
    void changeProductStatus_Fail_AlreadyInStatus() {
        testProduct.setStatus(ProductStatus.ACTIVE);
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> 
            productService.changeProductStatus(100L, ProductStatus.ACTIVE)
        );
        assertThat(ex.getMessage()).isEqualTo("Product is already in ACTIVE status.");
        verify(productRepository, never()).save(any());
    }

    // ==========================================
    // GET PRODUCT DETAILS TESTS
    // ==========================================

    @Test
    @DisplayName("Get Product Details: Success and calculates total stock")
    void getProductDetails_Success() {
        Inventory inv1 = Inventory.builder().quantity(10L).build();
        Inventory inv2 = Inventory.builder().quantity(20L).build();
        
        ProductVariant v1 = ProductVariant.builder().inventory(inv1).build();
        ProductVariant v2 = ProductVariant.builder().inventory(inv2).build();
        testProduct.getVariants().addAll(List.of(v1, v2));

        ProductDetailsResponse mockResponse = new ProductDetailsResponse();
        
        when(productRepository.findByIdAndStatus(100L, ProductStatus.ACTIVE)).thenReturn(Optional.of(testProduct));
        when(productMapper.toProductDetailsResponse(testProduct, 30L)).thenReturn(mockResponse);

        ProductDetailsResponse result = productService.getProductDetails(100L);

        assertThat(result).isNotNull();
        verify(productMapper, times(1)).toProductDetailsResponse(testProduct, 30L);
    }

    // ==========================================
    // GET VARIANT DETAILS TESTS
    // ==========================================

    @Test
    @DisplayName("Get Variant Details: Success matching attributes")
    void getVariantDetails_Success() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Color", "Red");
        attributes.put("Storage", "128GB");

        ProductVariant matchedVariant = ProductVariant.builder()
                .status(ProductStatus.ACTIVE)
                .attributes(attributes)
                .build();
        testProduct.getVariants().add(matchedVariant);

        VariantResponse mockVariantResponse = new VariantResponse();

        when(productRepository.findByIdAndStatus(100L, ProductStatus.ACTIVE)).thenReturn(Optional.of(testProduct));
        when(productMapper.toVariantResponse(matchedVariant)).thenReturn(mockVariantResponse);

        VariantResponse result = productService.getVariantDetails(100L, attributes);

        assertThat(result).isNotNull();
        verify(productMapper, times(1)).toVariantResponse(matchedVariant);
    }

    @Test
    @DisplayName("Get Variant Details: Fail when attributes don't match exactly")
    void getVariantDetails_Fail_AttributeMismatch() {
        Map<String, Object> dbAttributes = new HashMap<>();
        dbAttributes.put("Color", "Blue");

        ProductVariant variant = ProductVariant.builder()
                .status(ProductStatus.ACTIVE)
                .attributes(dbAttributes)
                .build();
        testProduct.getVariants().add(variant);

        Map<String, Object> searchAttributes = new HashMap<>();
        searchAttributes.put("Color", "Red"); // Mismatched

        when(productRepository.findByIdAndStatus(100L, ProductStatus.ACTIVE)).thenReturn(Optional.of(testProduct));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> 
            productService.getVariantDetails(100L, searchAttributes)
        );
        assertThat(ex.getMessage()).isEqualTo("Variant not found");
    }
}
