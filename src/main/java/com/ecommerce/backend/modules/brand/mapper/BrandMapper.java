package com.ecommerce.backend.modules.brand.mapper;

import com.ecommerce.backend.modules.brand.dto.request.BrandRequest;
import com.ecommerce.backend.modules.brand.dto.response.BrandResponse;
import com.ecommerce.backend.modules.brand.entity.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public Brand toEntity(BrandRequest request) {
        if (request == null) return null;

        return Brand.builder()
                .name(request.getName())
                .status(request.getStatus())
                .logo(request.getLogo())
                .build();
    }

    public BrandResponse toResponse(Brand brand) {
        if (brand == null) return null;

        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .logo(brand.getLogo())
                .status(brand.getStatus())
                .totalProducts((long) (brand.getProducts() != null ?
                        brand.getProducts().size() : 0))
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }
}
