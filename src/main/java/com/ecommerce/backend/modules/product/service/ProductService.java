package com.ecommerce.backend.modules.product.service;

import com.ecommerce.backend.modules.product.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ProductService {
    ProductDetailsResponse getProductDetails(Long id);

    VariantResponse getVariantDetails(Long productId, Map<String, Object> attributes);

    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);

    List<ProductCompareResponse> getProductsForComparison(List<Long> ids);

    List<SuggestionResponse> getSearchSuggestions(String keyword);
}
