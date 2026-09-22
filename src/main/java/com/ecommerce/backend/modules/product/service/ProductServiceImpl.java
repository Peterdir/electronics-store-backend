package com.ecommerce.backend.modules.product.service;

import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.product.dto.response.*;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.mapper.ProductMapper;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public ProductDetailsResponse getProductDetails(Long id) {
        Product product = productRepository.findByIdAndStatus(id, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Long totalStock = product.getVariants().stream()
                .map(variant -> variant.getInventory() != null ? variant.getInventory().getQuantity() : 0L)
                .reduce(0L, Long::sum);

        return productMapper.toProductDetailsResponse(product, totalStock);
    }

    @Override
    @Transactional(readOnly = true)
    public VariantResponse getVariantDetails(Long productId, Map<String, Object> attributes) {
        Product product = productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant matchedVariant = product.getVariants().stream()
                .filter(variant -> variant.getStatus() == ProductStatus.ACTIVE)
                .filter(variant -> matchAttributes(variant.getAttributes(), attributes))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        return productMapper.toVariantResponse(matchedVariant);
    }

    private boolean matchAttributes(Map<String, Object> variantAttributes, Map<String, Object> queryAttributes) {
        if (variantAttributes == null || queryAttributes == null || queryAttributes.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : queryAttributes.entrySet()) {
            Object variantValue = variantAttributes.get(entry.getKey());
            if (variantValue == null || !String.valueOf(variantValue).equals(String.valueOf(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        String searchKeyword = (keyword != null) ? keyword.trim() : "";

        Page<Product> productPage = productRepository.findByNameContainingIgnoreCaseAndStatus(
                searchKeyword,
                ProductStatus.ACTIVE,
                pageable
        );

        return productPage.map(productMapper::toProductResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCompareResponse> getProductsForComparison(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Product> products = productRepository.findAllByIdIn(ids);

        return products.stream()
                .map(productMapper::toProductCompareResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuggestionResponse> getSearchSuggestions(String keyword) {
        String searchKeyword = (keyword != null) ? keyword.trim() : "";
        if (searchKeyword.isEmpty()) {
            return List.of();
        }

        List<Product> topProducts = productRepository.findTop5ByNameContaining(searchKeyword);
        return topProducts.stream()
                .map(productMapper::toSuggestionResponse)
                .toList();
    }
}
