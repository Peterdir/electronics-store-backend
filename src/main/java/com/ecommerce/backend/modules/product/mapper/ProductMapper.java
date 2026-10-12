package com.ecommerce.backend.modules.product.mapper;

import com.ecommerce.backend.modules.product.dto.response.*;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductImage;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.List;

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
                        product.getProductImages().stream().map(ProductImage::getImageUrl).toList() : null)
                .variants(product.getVariants() != null ? 
                        product.getVariants().stream().map(this::toVariantResponse).toList() : null)
                .stock(totalStock)
                .averageRating(product.getAverageRating())
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
                .averageRating(product.getAverageRating())
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

    public AdminProductListResponse toAdminProductListResponse(Product product) {
        if (product == null) {
            return null;
        }

        Long totalStock = product.getVariants() != null
                ? product.getVariants().stream()
                .map(v -> v.getInventory() != null && v.getInventory().getQuantity() != null ? v.getInventory().getQuantity() : 0L)
                .reduce(0L, Long::sum)
                : 0L;

        int variantCount = product.getVariants() != null ? product.getVariants().size() : 0;

        return AdminProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .primaryImageUrl(getPrimaryImageUrl(product))
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .status(product.getStatus())
                .totalStock(totalStock)
                .variantCount(variantCount)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public AdminProductDetailResponse toAdminProductDetailResponse(Product product) {
        if (product == null) {
            return null;
        }

        Long totalStock = product.getVariants() != null
                ? product.getVariants().stream()
                .map(v -> v.getInventory() != null && v.getInventory().getQuantity() != null ? v.getInventory().getQuantity() : 0L)
                .reduce(0L, Long::sum)
                : 0L;

        List<ProductImageDetailResponse> imageResponses = product.getProductImages() != null
                ? product.getProductImages().stream()
                .map(this::toProductImageDetailResponse)
                .toList()
                : List.of();

        List<VariantResponse> variantResponses = product.getVariants() != null
                ? product.getVariants().stream()
                .map(this::toVariantResponse)
                .toList()
                : List.of();

        return AdminProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .description(product.getDescription())
                .specification(product.getSpecification())
                .status(product.getStatus())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .images(imageResponses)
                .variants(variantResponses)
                .totalStock(totalStock)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductImageDetailResponse toProductImageDetailResponse(ProductImage image) {
        if (image == null) {
            return null;
        }

        return ProductImageDetailResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .displayOrder(image.getDisplayOrder())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    private String getPrimaryImageUrl(Product product) {
        String primaryImageUrl = null;
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            primaryImageUrl = product.getProductImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(product.getProductImages().get(0).getImageUrl());
        }

        return primaryImageUrl;
    }
}
