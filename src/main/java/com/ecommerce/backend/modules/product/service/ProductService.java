package com.ecommerce.backend.modules.product.service;

import com.ecommerce.backend.modules.product.dto.response.*;
import com.ecommerce.backend.modules.product.dto.request.AdminProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import com.ecommerce.backend.modules.product.enums.ProductStatus;

public interface ProductService {
    void createProduct(AdminProductRequest request, List<MultipartFile> images);

    void updateProduct(Long id, AdminProductRequest request);

    void changeProductStatus(Long id, ProductStatus status);
    ProductDetailsResponse getProductDetails(Long id);

    VariantResponse getVariantDetails(Long productId, Map<String, Object> attributes);

    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);

    List<ProductCompareResponse> getProductsForComparison(List<Long> ids);

    List<SuggestionResponse> getSearchSuggestions(String keyword);
}
