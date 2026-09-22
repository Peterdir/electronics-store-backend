package com.ecommerce.backend.modules.product.mapper;

import com.ecommerce.backend.modules.product.dto.response.*;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductImage;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProductMapper {

    public ProductDetailsResponse toProductDetailsResponse(Product product, Long totalStock) {
        if (product == null) {
            return null;
        }

        return ProductDetailsResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .description(product.getDescription())
                .specification(product.getSpecification())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .images(product.getProductImages() != null ? 
                        product.getProductImages().stream().map(ProductImage::getImageUrl).collect(Collectors.toList()) : null)
                .variants(product.getVariants() != null ? 
                        product.getVariants().stream().map(this::toVariantResponse).collect(Collectors.toList()) : null)
                .stock(totalStock)
                .build();
    }

    public VariantResponse toVariantResponse(ProductVariant variant) {
        if (variant == null) {
            return null;
        }

        Long stock = (variant.getInventory() != null && variant.getInventory().getQuantity() != null) 
                ? variant.getInventory().getQuantity() : 0L;

        return VariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .stock(stock)
                .imageUrl(variant.getImageUrl())
                .attributes(variant.getAttributes())
                .status(variant.getStatus())
                .build();
    }

    public ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }

        String primaryImageUrl = getPrimaryImageUrl(product);

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .primaryImageUrl(primaryImageUrl)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .status(product.getStatus())
                .build();
    }

    public ProductCompareResponse toProductCompareResponse(Product product) {
        if (product == null) {
            return null;
        }

        String primaryImageUrl = getPrimaryImageUrl(product);

        return ProductCompareResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .imageUrl(primaryImageUrl)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .specification(product.getSpecification())
                .build();
    }

    public SuggestionResponse toSuggestionResponse(Product product) {
        if (product == null) {
            return null;
        }

        String primaryImageUrl = getPrimaryImageUrl(product);

        return SuggestionResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .imageUrl(primaryImageUrl)
                .build();
    }

    private String getPrimaryImageUrl(Product product) {
        String primaryImageUrl = null;
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            primaryImageUrl = product.getProductImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(product.getProductImages().getFirst().getImageUrl());
        }

        return primaryImageUrl;
    }
}
