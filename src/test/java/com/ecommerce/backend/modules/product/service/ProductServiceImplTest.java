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
import com.ecommerce.backend.modules.product.dto.request.AdminProductVariantRequest;
import com.ecommerce.backend.modules.product.dto.response.*;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.mapper.ProductMapper;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Unit Test Suite")
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

    // =========================================================================
    // AC-PROD-01: Tạo mới sản phẩm (createProduct)
    // =========================================================================
    @Nested
    @DisplayName("AC-PROD-01: Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("TC-PROD-01 [Positive]: Create Product without variants and images")
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
        @DisplayName("TC-PROD-02 [Positive]: Create Product with Brand, Variants and Images")
        void createProduct_Success_WithVariantsAndImages() {
            AdminProductRequest request = new AdminProductRequest();
            request.setName("iPhone 15 Pro");
            request.setBasePrice(new BigDecimal("1199"));
            request.setCategoryId(1L);
            request.setBrandId(1L);

            AdminProductVariantRequest variantReq = new AdminProductVariantRequest();
            variantReq.setSku("IP15P-256-TI");
            variantReq.setPrice(new BigDecimal("1299"));
            variantReq.setStock(50L);
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("color", "Titanium");
            variantReq.setAttributes(attrs);
            request.setVariants(List.of(variantReq));

            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn("image/png");
            when(mockFile.getOriginalFilename()).thenReturn("phone.png");
            when(mockFile.getSize()).thenReturn(1024L);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
            when(fileStorageService.uploadFile(mockFile, "products")).thenReturn("http://cdn/phone.png");

            productService.createProduct(request, List.of(mockFile));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository, times(1)).save(captor.capture());

            Product savedProduct = captor.getValue();
            assertEquals("iPhone 15 Pro", savedProduct.getName());
            assertEquals(1, savedProduct.getVariants().size());
            assertEquals("IP15P-256-TI", savedProduct.getVariants().get(0).getSku());
            assertEquals(50L, savedProduct.getVariants().get(0).getInventory().getQuantity());
            assertEquals(1, savedProduct.getProductImages().size());
            assertTrue(savedProduct.getProductImages().get(0).getIsPrimary());
        }

        @Test
        @DisplayName("TC-PROD-03 [Negative]: Throw ResourceNotFoundException when Category is missing")
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

        @Test
        @DisplayName("TC-PROD-04 [Negative]: Throw ResourceNotFoundException when Brand is missing")
        void createProduct_Fail_BrandNotFound() {
            AdminProductRequest request = new AdminProductRequest();
            request.setCategoryId(1L);
            request.setBrandId(99L);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                    productService.createProduct(request, null)
            );
            assertThat(ex.getMessage()).isEqualTo("Brand not found");
            verify(productRepository, never()).save(any());
        }
    }

    // =========================================================================
    // AC-PROD-02: Cập nhật sản phẩm (updateProduct)
    // =========================================================================
    @Nested
    @DisplayName("AC-PROD-02: Update Product Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("TC-PROD-05 [Positive]: Update Product basic info and existing variant")
        void updateProduct_Success_UpdateExistingVariant() {
            ProductVariant existingVariant = ProductVariant.builder()
                    .sku("SKU-01")
                    .price(new BigDecimal("100"))
                    .inventory(Inventory.builder().quantity(10L).build())
                    .build();
            testProduct.getVariants().add(existingVariant);

            AdminProductRequest request = new AdminProductRequest();
            request.setName("Updated iPhone");
            request.setBasePrice(new BigDecimal("1099"));
            request.setCategoryId(1L);
            request.setBrandId(1L);

            AdminProductVariantRequest variantReq = new AdminProductVariantRequest();
            variantReq.setSku("SKU-01");
            variantReq.setPrice(new BigDecimal("120"));
            variantReq.setStock(25L);
            request.setVariants(List.of(variantReq));

            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

            productService.updateProduct(100L, request);

            assertEquals("Updated iPhone", testProduct.getName());
            assertEquals(new BigDecimal("120"), existingVariant.getPrice());
            assertEquals(25L, existingVariant.getInventory().getQuantity());
            verify(productRepository, times(1)).save(testProduct);
        }

        @Test
        @DisplayName("TC-PROD-06 [Positive]: Update Product adding new variant")
        void updateProduct_Success_AddNewVariant() {
            AdminProductRequest request = new AdminProductRequest();
            request.setName("iPhone 15");
            request.setBasePrice(new BigDecimal("999"));
            request.setCategoryId(1L);

            AdminProductVariantRequest newVariantReq = new AdminProductVariantRequest();
            newVariantReq.setSku("SKU-NEW");
            newVariantReq.setPrice(new BigDecimal("150"));
            newVariantReq.setStock(30L);
            request.setVariants(List.of(newVariantReq));

            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            productService.updateProduct(100L, request);

            assertEquals(1, testProduct.getVariants().size());
            assertEquals("SKU-NEW", testProduct.getVariants().get(0).getSku());
            assertEquals(30L, testProduct.getVariants().get(0).getInventory().getQuantity());
            verify(productRepository, times(1)).save(testProduct);
        }

        @Test
        @DisplayName("TC-PROD-07 [Negative]: Throw ResourceNotFoundException when Product not found")
        void updateProduct_Fail_ProductNotFound() {
            AdminProductRequest request = new AdminProductRequest();
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.updateProduct(999L, request)
            );
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-PROD-08 [Negative]: Throw ResourceNotFoundException when Category not found")
        void updateProduct_Fail_CategoryNotFound() {
            AdminProductRequest request = new AdminProductRequest();
            request.setCategoryId(99L);
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.updateProduct(100L, request)
            );
        }

        @Test
        @DisplayName("TC-PROD-09 [Negative]: Throw ResourceNotFoundException when Brand not found")
        void updateProduct_Fail_BrandNotFound() {
            AdminProductRequest request = new AdminProductRequest();
            request.setCategoryId(1L);
            request.setBrandId(99L);
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.updateProduct(100L, request)
            );
        }
    }

    // =========================================================================
    // AC-PROD-03: Đổi trạng thái sản phẩm (changeProductStatus)
    // =========================================================================
    @Nested
    @DisplayName("AC-PROD-03: Change Product Status Tests")
    class ChangeProductStatusTests {

        @Test
        @DisplayName("TC-PROD-10 [Positive]: Change Status to INACTIVE successfully")
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
        @DisplayName("TC-PROD-11 [Negative]: Fail if product is already in that status")
        void changeProductStatus_Fail_AlreadyInStatus() {
            testProduct.setStatus(ProductStatus.ACTIVE);
            when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    productService.changeProductStatus(100L, ProductStatus.ACTIVE)
            );
            assertThat(ex.getMessage()).isEqualTo("Product is already in ACTIVE status.");
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-PROD-12 [Negative]: Fail when Product does not exist")
        void changeProductStatus_Fail_NotFound() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.changeProductStatus(999L, ProductStatus.INACTIVE)
            );
        }
    }

    // =========================================================================
    // AC-PROD-04: Xem chi tiết sản phẩm (getProductDetails)
    // =========================================================================
    @Nested
    @DisplayName("AC-PROD-04: Get Product Details Tests")
    class GetProductDetailsTests {

        @Test
        @DisplayName("TC-PROD-13 [Positive]: Get Product Details calculates total stock")
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

        @Test
        @DisplayName("TC-PROD-14 [Negative]: Throw ResourceNotFoundException when product not active or not found")
        void getProductDetails_NotFound() {
            when(productRepository.findByIdAndStatus(999L, ProductStatus.ACTIVE)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.getProductDetails(999L)
            );
        }
    }

    // =========================================================================
    // AC-PROD-05: Xem chi tiết biến thể (getVariantDetails)
    // =========================================================================
    @Nested
    @DisplayName("AC-PROD-05: Get Variant Details Tests")
    class GetVariantDetailsTests {

        @Test
        @DisplayName("TC-PROD-15 [Positive]: Success matching attributes")
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
        @DisplayName("TC-PROD-16 [Negative]: Fail when attributes don't match exactly")
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

        @Test
        @DisplayName("TC-PROD-17 [Boundary]: Fail when query attributes are empty or null")
        void getVariantDetails_Fail_EmptyOrNullAttributes() {
            when(productRepository.findByIdAndStatus(100L, ProductStatus.ACTIVE)).thenReturn(Optional.of(testProduct));

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.getVariantDetails(100L, Collections.emptyMap())
            );

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.getVariantDetails(100L, null)
            );
        }
    }

    // =========================================================================
    // AC-PROD-06: Tìm kiếm sản phẩm (searchProducts)
    // =========================================================================
    @Nested
    @DisplayName("AC-PROD-06: Search Products Tests")
    class SearchProductsTests {

        @Test
        @DisplayName("TC-PROD-18 [Positive]: Search products with keyword returns page")
        void searchProducts_WithKeyword_ReturnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));
            ProductResponse productResponse = new ProductResponse();

            when(productRepository.findByNameContainingIgnoreCaseAndStatus("iPhone", ProductStatus.ACTIVE, pageable))
                    .thenReturn(productPage);
            when(productMapper.toProductResponse(testProduct)).thenReturn(productResponse);

            Page<ProductResponse> result = productService.searchProducts("  iPhone  ", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(productRepository, times(1)).findByNameContainingIgnoreCaseAndStatus("iPhone", ProductStatus.ACTIVE, pageable);
        }

        @Test
        @DisplayName("TC-PROD-19 [Boundary]: Search products with null keyword defaults to empty string")
        void searchProducts_NullKeyword_DefaultsToEmptyString() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(Collections.emptyList());

            when(productRepository.findByNameContainingIgnoreCaseAndStatus("", ProductStatus.ACTIVE, pageable))
                    .thenReturn(productPage);

            Page<ProductResponse> result = productService.searchProducts(null, pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // =========================================================================
    // AC-PROD-07: So sánh sản phẩm (getProductsForComparison)
    // =========================================================================
    @Nested
    @DisplayName("AC-PROD-07: Compare Products Tests")
    class CompareProductsTests {

        @Test
        @DisplayName("TC-PROD-20 [Positive]: Return comparison response for valid product IDs")
        void getProductsForComparison_ValidIds_ReturnsList() {
            List<Long> ids = List.of(100L, 101L);
            when(productRepository.findAllByIdIn(ids)).thenReturn(List.of(testProduct));
            when(productMapper.toProductCompareResponse(testProduct)).thenReturn(new ProductCompareResponse());

            List<ProductCompareResponse> result = productService.getProductsForComparison(ids);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(productRepository, times(1)).findAllByIdIn(ids);
        }

        @Test
        @DisplayName("TC-PROD-21 [Boundary]: Return empty list when IDs is null or empty")
        void getProductsForComparison_NullOrEmptyIds_ReturnsEmpty() {
            assertTrue(productService.getProductsForComparison(null).isEmpty());
            assertTrue(productService.getProductsForComparison(Collections.emptyList()).isEmpty());
            verify(productRepository, never()).findAllByIdIn(any());
        }
    }

    // =========================================================================
    // AC-PROD-08: Gợi ý tìm kiếm (getSearchSuggestions)
    // =========================================================================
    @Nested
    @DisplayName("AC-PROD-08: Search Suggestions Tests")
    class SearchSuggestionsTests {

        @Test
        @DisplayName("TC-PROD-22 [Positive]: Return suggestions when keyword is provided")
        void getSearchSuggestions_WithKeyword_ReturnsSuggestions() {
            SuggestionResponse suggestion = new SuggestionResponse();
            when(productRepository.findTop5ByNameContaining("phone")).thenReturn(List.of(testProduct));
            when(productMapper.toSuggestionResponse(testProduct)).thenReturn(suggestion);

            List<SuggestionResponse> result = productService.getSearchSuggestions("  phone  ");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(productRepository, times(1)).findTop5ByNameContaining("phone");
        }

        @Test
        @DisplayName("TC-PROD-23 [Boundary]: Return empty list when keyword is empty or null")
        void getSearchSuggestions_EmptyKeyword_ReturnsEmptyList() {
            assertTrue(productService.getSearchSuggestions("   ").isEmpty());
            assertTrue(productService.getSearchSuggestions(null).isEmpty());
            verify(productRepository, never()).findTop5ByNameContaining(anyString());
        }
    }
}
